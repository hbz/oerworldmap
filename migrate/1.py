__author__ = 'fo'

import sys, getopt, json, os

def migrate(input_dir, output_dir):
    print 'Input dir is ', input_dir
    print 'Output dir is ', output_dir
    for inputfile in os.listdir(input_dir):
        input_path = os.path.join(input_dir, inputfile)
        with open(input_path) as data_file:
            data = remove_ids(json.load(data_file))
            output_path = os.path.join(output_dir, inputfile)
            with open(output_path, 'w') as migrated_file:
                json.dump(data, migrated_file)

def remove_ids(json_data):
    for i in json_data.keys():
        if ("@id" == i):
            json_data.pop(i)
        elif (isinstance(json_data[i], dict)):
            json_data[i] = remove_ids(json_data[i])
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
