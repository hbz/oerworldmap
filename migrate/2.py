__author__ = 'fo'

import sys, getopt, json, os

def migrate(input_dir, output_dir):
    print 'Input dir is ', input_dir
    print 'Output dir is ', output_dir
    for inputfile in os.listdir(input_dir):
        input_path = os.path.join(input_dir, inputfile)
        with open(input_path) as data_file:
            data = fix_dates(json.load(data_file))
            output_path = os.path.join(output_dir, data['about']['@type'])
            if not os.path.exists(output_path):
                os.makedirs(output_path)
            output_file = os.path.join(output_path, inputfile)
            with open(output_file, 'w') as migrated_file:
                json.dump(data, migrated_file)

def fix_dates(json_data):
    try:
        json_data['dateCreated'] = json_data['dateCreated'].split('.')[0] + 'Z'
    except KeyError:
        pass
    try:
        json_data['dateModified'] = json_data['dateModified'].split('.')[0] + 'Z'
    except KeyError:
        pass
    return json_data

if __name__ == "__main__":
    input_dir = ''
    output_dir = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:],"hi:o:",["idir=","odir="])
    except getopt.GetoptError:
        print '1.py -i <inputdir> -o <outputdir>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -i <inputdir> -o <outputdir>'
            sys.exit()
        elif opt in ("-i", "--idir"):
            input_dir = arg
        elif opt in ("-o", "--odir"):
            output_dir = arg
    migrate(input_dir, output_dir)
