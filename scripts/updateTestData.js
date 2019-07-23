var stdin = process.stdin,
    stdout = process.stdout,
    inputChunks = [],
    fields = ['name', 'alternateName', 'articleBody', 'description', 'displayName', 'scopeNote', 'text']

stdin.resume()
stdin.setEncoding('utf8')

stdin.on('data', function (chunk) {
    inputChunks.push(chunk)
})

stdin.on('end', function () {
    var inputJSON = inputChunks.join(),
        parsedData = JSON.parse(inputJSON),
        outputJSON = JSON.stringify(parsedData, null, 2)
    //stdout.write(outputJSON)
    //stdout.write('\n')
    console.log(JSON.stringify(update(parsedData), null, 2))
})

const update = node => {
  if (!node) return node
  if (node.constructor === Object) {
    Object.entries(node).forEach(([key, value]) => {
      if (fields.includes(key) && value.constructor === Array) {
        node[key] = value.reduce((acc, curr) => {
          acc[curr['@language']] = curr['@value']
          return acc
        }, {})
      } else if ('location' === key && value.constructor === Object) {
        node[key] = [value]
      } else {
        node[key] = update(value)
      }
    })
    return node
  } else if (node.constructor === Array) {
    return node.map(n => update(n))
  } else {
    return node
  }
}
