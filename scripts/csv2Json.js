const csv = require('csv-parser')
const pointer = require('json-pointer')

const items = []
let isHeader = true
const headers = []
process.stdin.pipe(csv({headers: false})).on('data', function(row) {
  try {
    if (isHeader) {
      for (const [key, value] of Object.entries(row)) {
        headers[key] = value
      }
      isHeader = false
      return
    }
    item = {}
    for (const [key, value] of Object.entries(row)) {
      if (!!value)
        pointer.set(item, headers[key], value)
    }
    items.push(item)
  } catch (err) {
    console.error(err)
  }
}).on('end', function() {
  console.log(JSON.stringify(items))
})
