let config = {};
let currentEnvironment = 'sandbox';

window.addEventListener('DOMContentLoaded', async () => {
  await checkConfig();
  await loadConfig();
  setupEventListeners();
});

function setupEventListeners() {
  document.getElementById('config-form').addEventListener('submit', saveConfig);
  window.addEventListener('message', handleMessage);
}

function navigateToConfig() {
  document.getElementById('home-page').style.display = 'none';
  document.getElementById('config-page').style.display = 'block';
}

function navigateToHome() {
  document.getElementById('config-page').style.display = 'none';
  document.getElementById('home-page').style.display = 'block';
}

async function checkConfig() {
  try {
    const response = await fetch('/api/health');
    const data = await response.json();
    if (!data.success || !data.configComplete) {
      navigateToConfig();
    }
  } catch (error) {
    showMessage('检查配置失败', 'error');
  }
}

async function loadConfig() {
  try {
    const response = await fetch('/api/config');
    const data = await response.json();
    if (data.success) {
      config = data.data;
      populateConfigForm();
    }
  } catch (error) {
    showMessage('加载配置失败', 'error');
  }
}

function populateConfigForm() {
  const fields = ['SANDBOX_SITE', 'PROD_SITE', 'FIDO_INIT', 'FIDO_CHALLENGE', 
                  'JWT_API_KEY_ID', 'JWT_ORG_UNIT_ID', 'JWT_SECRET', 
                  'MERCHANT_ORIGIN', 'RETURN_URL'];
  
  fields.forEach(field => {
    const input = document.getElementById(field);
    if (input && config[field]) {
      input.value = config[field];
    }
  });
}

async function saveConfig(e) {
  e.preventDefault();
  
  const saveBtn = document.getElementById('save-btn');
  saveBtn.disabled = true;
  saveBtn.textContent = '保存中...';
  
  try {
    const formData = new FormData(e.target);
    const configData = {};
    
    for (let [key, value] of formData.entries()) {
      configData[key] = value;
    }
    
    const response = await fetch('/api/config', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(configData)
    });
    
    const data = await response.json();
    
    if (data.success) {
      showConfigMessage('配置保存成功', 'success');
      config = configData;
      setTimeout(() => {
        navigateToHome();
      }, 1000);
    } else {
      showConfigMessage(data.error, 'error');
    }
  } catch (error) {
    showConfigMessage('保存配置失败', 'error');
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = '保存配置';
  }
}

async function runCompatibilityCheck() {
  const runBtn = document.getElementById('runBtn');
  runBtn.disabled = true;
  runBtn.textContent = '执行中...';
  
  clearMessage();
  hideResults();
  
  try {
    const response = await fetch('/api/jwt/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    const data = await response.json();
    
    if (!data.success) {
      throw new Error(data.error);
    }
    
    const { jws, payload } = data.data;
    document.getElementById('request-jwt').textContent = jws;
    document.getElementById('request-payload').textContent = formatJson(payload);
    document.getElementById('request-section').style.display = 'block';
    
    const siteUrl = currentEnvironment === 'sandbox' ? config.SANDBOX_SITE : config.PROD_SITE;
    const fidoInitUrl = `${siteUrl}${config.FIDO_INIT}`;
    
    const iframe = document.getElementById('fidoIframe');
    iframe.src = `/fido-form.html?jwt=${encodeURIComponent(jws)}&action=${encodeURIComponent(fidoInitUrl)}`;
    document.getElementById('iframe-container').style.display = 'block';
    
  } catch (error) {
    showMessage(`执行失败: ${error.message}`, 'error');
  } finally {
    runBtn.disabled = false;
    runBtn.textContent = 'Compatibility Check';
  }
}

function handleMessage(event) {
  if (event.data.type === 'JWT_RESPONSE') {
    const { data, jws } = event.data;
    document.getElementById('response-jws').textContent = jws;
    document.getElementById('response-payload').textContent = formatJson(data);
    document.getElementById('response-section').style.display = 'block';
    
    if (data.Payload.ErrorNumber === 0) {
      showMessage('Compatibility Check Successful', 'success');
    } else {
      showMessage(`Compatibility Check Failed: ${data.Payload.ErrorDescription}`, 'error');
    }
    
    document.getElementById('iframe-container').style.display = 'none';
  }
}

function formatJson(obj) {
  return JSON.stringify(obj, null, 2);
}

function showMessage(text, type) {
  const messageEl = document.getElementById('message');
  messageEl.textContent = text;
  messageEl.className = `message ${type}`;
}

function clearMessage() {
  const messageEl = document.getElementById('message');
  messageEl.textContent = '';
  messageEl.className = 'message';
}

function showConfigMessage(text, type) {
  const messageEl = document.getElementById('config-message');
  messageEl.textContent = text;
  messageEl.className = `message ${type}`;
}

function hideResults() {
  document.getElementById('iframe-container').style.display = 'none';
  document.getElementById('request-section').style.display = 'none';
  document.getElementById('response-section').style.display = 'none';
}

document.getElementById('environment').addEventListener('change', (e) => {
  currentEnvironment = e.target.value;
});
