import re

def migrate(resource):
    return isced_to_object(resource)

def isced_to_object(resource):

    for property in resource.keys():
        if property in ["audience", "about"]:
            print "Migrating " + property + " for " + resource["@id"]
            objects = []
            for string in resource[property]:
                object = string_to_object(string)
                if object:
                    objects.append(object)
            resource[property] = objects
        elif isinstance(resource[property], list):
            for i, val in enumerate(resource[property]):
                if isinstance(val, dict):
                    resource[property][i] = isced_to_object(val)
        elif isinstance(resource[property], dict):
            resource[property] = isced_to_object(resource[property])

    return resource

def string_to_object(string):
    pattern = re.compile('\(ISCED-(.+)\)')
    match = pattern.search(string)
    if match:
        try:
            classification, concept = match.group(1).split(":")
        except ValueError:
            print "Erroneous ISCED ID " + string + ", skipping"
            return None
        id = None
        if classification == "2013":
            id = "https://w3id.org/class/esc/n" + concept
        elif classification == "1997":
            id = "https://w3id.org/isced/1997/level" + concept
        if id:
            return {
                "@id": id
            }
        else:
            return None
    else:
        print "No match for " + string
        return None
