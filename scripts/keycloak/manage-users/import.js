const KcAdminClient = require('keycloak-admin').default
const jlog = require('json-colorz')
const fs = require("fs")
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
  const HTPROFILES = await qoa.input({
    query: 'htprofiles file:',
    handle: 'HTPROFILES'
  })
  const config = Object.assign({}, ADMIN_REALM, ADMIN_USER, ADMIN_PASS, USER_REALM, HTPROFILES)

  const lines = fs.readFileSync(config.HTPROFILES,'utf8').trim().split('\n')
  const users = lines.map(line => {
    const [ email, profile_id ] = line.split(" ")
    return {
      email,
      profile_id
    }
  })
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

  const q = await qoa.prompt([{
    type: 'confirm',
    query: `Do you want to create ${users.length} users?`,
    handle: 'confirm',
    accept: 'Y',
    deny: 'n'
  }])

  if (q.confirm) {
    for (user of users) {
      try {
        console.log("Creating user:", user.email, user.profile_id)

        await keycloakAdmin.users.create({
          realm: config.USER_REALM,
          username: user.email,
          email: user.email,
          emailVerified: true,
          enabled: true,
          firstName: "OER World Map",
          lastName: "Legacy User",
          attributes: {
            profile_id: user.profile_id
          },
        })

      } catch (error) {
        console.error("Failed creation of user:", error)
      }
    }
  } else {
    const allUsers = await keycloakAdmin.users.find({
      max: 10000
    })
    jlog(allUsers)
  }
}

init()
