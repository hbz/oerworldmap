import re

def migrate(resource):
    return isced_to_object(resource)

def isced_to_object(resource):
    if "audience" in resource:
        print "Migrating audience for " + resource["@id"]
        objects = []
        for string in resource["audience"]:
            object = string_to_object(string)
            if object:
                objects.append(object)
        resource["audience"] = objects

    if "about" in resource:
        print "Migrating about for " + resource["@id"]
        objects = []
        for string in resource["about"]:
            object = string_to_object(string)
            if object:
                objects.append(object)
        resource["about"] = objects

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
