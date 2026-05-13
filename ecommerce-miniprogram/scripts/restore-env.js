const fs = require('fs')
const path = require('path')

const envPath = path.resolve(__dirname, '..', '.env')
const manifestPath = path.resolve(__dirname, '..', 'manifest.json')

let env = {}
if (fs.existsSync(envPath)) {
  const envContent = fs.readFileSync(envPath, 'utf-8')
  envContent.split('\n').forEach(line => {
    const [key, ...rest] = line.split('=')
    if (key && rest.length) env[key.trim()] = rest.join('=').trim()
  })
}

let manifest = fs.readFileSync(manifestPath, 'utf-8')
const appid = env.WX_APPID || ''
if (appid) {
  manifest = manifest.replace(new RegExp(appid, 'g'), '__WX_APPID__')
}

fs.writeFileSync(manifestPath, manifest, 'utf-8')
console.log('env restored')
