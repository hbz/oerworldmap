def migrate(resource):
    return remove_referenced_by(resource)

def remove_referenced_by(resource):
    for property in resource.keys():
        if "referencedBy" == property:
            print "Removing referencedBy from " + resource['@id']
            resource.pop(property)
        elif isinstance(resource[property], list):
            for i, val in enumerate(resource[property]):
                if isinstance(val, dict):
                    resource[property][i] = remove_referenced_by(val)
        elif isinstance(resource[property], dict):
            resource[property] = remove_referenced_by(resource[property])
    return resource
