def migrate(resource):
    return localized_strings_to_array(resource)

def localized_strings_to_array(resource):
    for property in resource.keys():
        # FIXME: why on earth does the schema not accept affiliation arrays???
        if property ==  "affiliation":
            del resource[property]
        elif property in [ "description", "alternateName"] and not isinstance(resource[property], list):
            print "Migrating " + property + " to list for " + resource['@id']
            if isinstance(resource[property], dict):
                resource[property] = [resource[property]]
            else:
                resource[property] = [{
                    "@value": resource[property],
                    "@language": "en"
                }]
        elif isinstance(resource[property], list):
            for i, val in enumerate(resource[property]):
                if isinstance(val, dict):
                    resource[property][i] = localized_strings_to_array(val)
        elif isinstance(resource[property], dict):
            resource[property] = localized_strings_to_array(resource[property])
    return resource
