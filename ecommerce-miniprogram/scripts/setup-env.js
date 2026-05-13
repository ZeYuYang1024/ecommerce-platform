const fs = require('fs')
const path = require('path')

const envPath = path.resolve(__dirname, '..', '.env')
const manifestPath = path.resolve(__dirname, '..', 'manifest.json')

const envContent = fs.readFileSync(envPath, 'utf-8')
const env = {}
envContent.split('\n').forEach(line => {
  const [key, ...rest] = line.split('=')
  if (key && rest.length) env[key.trim()] = rest.join('=').trim()
})

let manifest = fs.readFileSync(manifestPath, 'utf-8')
manifest = manifest.replace(/__WX_APPID__/g, env.WX_APPID || '__WX_APPID__')

fs.writeFileSync(manifestPath, manifest, 'utf-8')
console.log('env setup done')
