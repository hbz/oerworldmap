import uuid

def migrate(resource):
    return isced_to_object(resource)

def isced_to_object(resource):
    if "audience" in resource:
        print "Removing audience from " + resource["@id"]
        del resource["audience"]
    if "about" in resource:
        print "Removing about from " + resource["@id"]
        del resource["about"]
    # id_map = {}
    # for property in resource.keys():
    #     if property in ["audience", "about"]:
    #         objects = []
    #         for label in resource[property]:
    #             try:
    #                 id = id_map[label]
    #             except KeyError:
    #                 id = uuid.uuid1().urn
    #                 id_map[label] = id
    #             print "Migrating " + label + " to object with id " + id
    #             objects.append({"@id": id, "@type": "Concept", "prefLabel": [{"@value": label, "@language": "en"}]})
    #         resource[property] = objects
    #     elif isinstance(resource[property], dict):
    #         resource[property] = isced_to_object(resource[property])
    return resource
