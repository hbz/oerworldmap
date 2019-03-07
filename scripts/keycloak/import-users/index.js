const KcAdminClient = require('keycloak-admin').default
const jlog = require('json-colorz')
const fs = require("fs")
const qoa = require('qoa')
const { log, warn } = console

const keycloakAdmin = new KcAdminClient({
  baseUrl: 'http://127.0.0.1:8080/auth'
  // realmName: 'master'
})

keycloakAdmin.auth({
  username: 'admin',
  password: 'pass',
  'grantType': 'password',
  'clientId': 'admin-cli'
}).then(async () => {

  var text = fs.readFileSync('htprofiles','utf8')
  const lines = text.trim().split('\n')

  const users = []
  for (line of lines) {
    const [ email, profile_id ] = line.split(" ")
    users.push({
      email,
      profile_id
    })
  }

  const confirm = {
    type: 'confirm',
    query: `Do you want to create ${users.length} users?`,
    handle: 'confirm',
    accept: 'Y',
    deny: 'n'
  }

  qoa.prompt([confirm]).then(async (q) => {
    console.log(q);

    if (q.confirm) {
      for (user of users) {
        try {
          log("Creating user:", user.email, user.profile_id)

          await keycloakAdmin.users.create({
            // realm: 'master',
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
          warn("Failed creation of user:", user.email, user.profile_id)
          // console.log(error)
        }
      }
    } else {
      const allUsers = await keycloakAdmin.users.find()
      jlog(allUsers)
    }
  })
})