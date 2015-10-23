import csv, json, getopt, sys, urllib2, rdflib, pycountry, uuid, os, jsonschema

def convert(input_path, output_path):
    with open('../../public/json/schema.json') as schema_file:
        schema = json.load(schema_file)
    with open(input_path) as csvfile:
        servicereader = csv.reader(csvfile, delimiter=',', quotechar='"')
        # skip the first line
        next(servicereader)
        for row in servicereader:
            oerwm_id = 'urn:uuid:' + str(uuid.uuid1())
            if not row[0].strip():
                continue
            try:
                country = pycountry.countries.get(name=row[8].strip()).alpha2
            except KeyError:
                country = None
            service = {
                "@context": "http://schema.org/",
                "@id": oerwm_id,
                "@type": "Service",
                "name": [
                    {
                        "@language": "en",
                        "@value": row[0].strip()
                    }
                ],
                "description": [
                    {
                        "@language": "en",
                        "@value": row[3].strip()
                    }
                ],
                "availableChannel": [
                    {
                        "@type": "ServiceChannel",
                        "serviceUrl": row[5].strip(),
                        "availableLanguage": [a.strip() for a in row[10].split(',')]
                    }
                ],
            }

            if row[1].strip():
                service["alternateName"] = [
                    {
                        "@language": "en",
                        "@value": row[1].strip()
                    }
                ]

            if row[2].strip():
                service["image"] = row[2].strip()

            if row[9].strip():
                service["email"] = row[9].strip()

            if row[4].strip():
                service["startDate"] = row[4].strip()

            if row[6].strip():
                service["sameAs"] = [a.strip() for a in row[6].split('\n')]

            if row[7].strip():
                service["license"] = [a.strip() for a in row[7].split(',')]

            if row[11].strip():
                service["audience"] = [{"@id": "https://w3id.org/isced/1997/level" + a.strip()} for a in row[11].split(',') if a.strip().isdigit()]

            if row[12].strip():
                service["about"] = [{"@id": "https://w3id.org/class/esc/n" + a.strip()} for a in row[12].split(',') if a.strip().isdigit()]

            if row[13].strip():
                service["provider"] = [
                    {
                        "@id": "urn:uuid:" + row[13].strip()
                    }
                ]
            else:
                service["provider"] = [
                    {
                        "@type": "Organization",
                        "name": [
                            {
                                "@language": "en",
                                "@value": row[14].strip()
                            }
                        ],
                        "alternateName": [
                            {
                                "@language": "en",
                                "@value": row[15].strip()
                            },
                        ],
                        "description": [
                            {
                                "@language": "en",
                                "@value": row[17].strip()
                            }
                        ],
                        "sameAs": [a.strip() for a in row[19].split('\n')]
                    }
                ]

                if row[16].strip():
                    service["provider"][0]["image"] = row[16].strip()

                if row[18].strip():
                    service["provider"][0]["url"] = row[18].strip()

                if row[25].strip():
                    service["provider"][0]["email"] = row[25].strip()

                try:
                    pycountry.countries.get(alpha2=row[20].strip())
                    service["provider"][0]["location"] = {
                        "@type": "Place",
                        "address": {
                            "@type": "PostalAddress",
                            "addressCountry": row[20].strip(),
                            "addressLocality": row[21].strip(),
                            "addressRegion": row[22].strip(),
                            "streetAddress": row[23].strip(),
                            "postalCode": row[24].strip()
                        }
                    }
                except KeyError:
                    pass

                #if row[26].strip():
                #service["tag"] = [row[26].strip()]

            try:
                jsonschema.validate(service, schema, format_checker=jsonschema.FormatChecker())
                output_file = os.path.join(output_path, oerwm_id)
                with open(output_file, 'w') as file:
                    json.dump(service, file, indent=2)
                print "Wrote data for " + oerwm_id
            except jsonschema.ValidationError as validation_error:
                print "Validation failed for " + oerwm_id
                print validation_error.message

if __name__ == "__main__":
    input_path = ''
    output_path = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hi:o:", ["ifile=", "ofile="])
    except getopt.GetoptError:
        print '1.py -i <inputfile> -o <outputfile>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -i <inputfile> -o <outputfile>'
            sys.exit()
        elif opt in ("-i", "--ifile"):
            input_path = arg
        elif opt in ("-o", "--ofile"):
            output_path = arg
    convert(input_path, output_path)
