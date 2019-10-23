const KcAdminClient = require('keycloak-admin').default
const jlog = require('json-colorz')
const qoa = require('qoa')

const init = async () => {
  const ADMIN_REALM = await qoa.input({
    query: 'Admin realm:',
    handle: 'ADMIN_REALM'
  })
  const ADMIN_USER = await qoa.input({
    query: 'Admin user:',
    handle: 'ADMIN_USER'
  })
  const ADMIN_PASS = await qoa.secure({
    query: 'Admin password:',
    handle: 'ADMIN_PASS'
  })
  const USER_REALM = await qoa.input({
    query: 'User realm:',
    handle: 'USER_REALM'
  })
  const config = Object.assign({}, ADMIN_REALM, ADMIN_USER, ADMIN_PASS, USER_REALM)

  const keycloakAdmin = new KcAdminClient({
    baseUrl: 'http://127.0.0.1:8080/auth',
    realmName: config.ADMIN_REALM
  })

  await keycloakAdmin.auth({
    username: config.ADMIN_USER,
    password: config.ADMIN_PASS,
    'grantType': 'password',
    'clientId': 'admin-cli'
  })

  keycloakAdmin.setConfig({
    realmName: config.USER_REALM,
  })

  const users = await keycloakAdmin.users.find({
    max: 10000
  })

  const q = await qoa.prompt([{
    type: 'confirm',
    query: `Do you want to delete all ${users.length} users?`,
    handle: 'confirm',
    accept: 'Y',
    deny: 'n'
  }])
  console.log(q)
  if (q.confirm) {
    for (user of users) {
      try {
        await keycloakAdmin.users.del(user)
      } catch (error) {
        console.error("Failed deleting user", error)
      }
    }
  } else {
    const allUsers = await keycloakAdmin.users.find()
    jlog(users)
  }
}

init()
