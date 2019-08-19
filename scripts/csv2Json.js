const csv = require('csv-parser')
const pointer = require('json-pointer')

const items = []
process.stdin.pipe(csv()).on('data', function(row) {
  try {
    item = {}
    for (const [key, value] of Object.entries(row)) {
      if (!!value)
        pointer.set(item, key, value)
    }
    items.push(item)
  } catch (err) {
    console.error(err)
  }
}).on('end', function() {
  console.log(JSON.stringify(items))
})
