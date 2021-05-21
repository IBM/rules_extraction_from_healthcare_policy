#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Utility function to filter external data CSV files by CSC code
Generates output of:
- a subset procedures-csv file filtered by specified CSC code
- a csv of the groups (including members) taken from procgroups contained in the data subset
"""

import sys
import csv
import pandas as pd
from re import sub


def snake(s):
    s = s.replace(",", "_")
    s = s.replace("/", "_")
    s = '_'.join(
        sub('([A-Z][a-z]+)', r' \1',
            sub('([A-Z]+)', r' \1',
                s.replace('-', ' '))).split()).lower()
    return 'http://claims-audit.ibm.com/custom_procedure_codes_group_' + s


def filter_procedures_csv(filename, output, code):
    df = pd.read_csv(filename, sep=',')
    df2 = df[df['CSC'] == code]
    print("%s procedure entities with matching code found, writing to csv..." % df2.shape[0])
    df2.to_csv(output, index=False, header=True)


def generate_procgroups_csv(filename, output_groups, code):
    df = pd.read_csv(filename, sep=',')
    filtered_df = df[df['CSC'] == code]
    df2 = pd.DataFrame({'Procgroup code':filtered_df['Procgroup code'].unique()})
    namespace = "http://claims-audit.ibm.com/"
    tag = "procedure-codes-supplies.20210113"
    typesId = "http://claims-audit.ibm.com/CustomProcedureCodesGroup"

    rows = []
    for index, row in df2.iterrows():
        description = ', '.join(set(filtered_df['Procgroup title'].loc[filtered_df['Procgroup code'] == row['Procgroup code']]))
        group = {
            'description': description,
            'displayName': description,
            'entityId': snake(description),
            'members': str([', '.join(list(set(filtered_df['hcpcscpt'].loc[filtered_df['Procgroup code'] == row['Procgroup code']])))]).replace("'",""),
            'namespace': namespace,
            'tag': tag,
            'typesId': typesId,
        }
        rows.append(group.copy())

    df3 = pd.DataFrame(rows)
    print("%s procedure groups identified, writing to csv..." % df3.shape[0])
    df3.to_csv(output_groups, index=False, header=True, quoting=csv.QUOTE_ALL)


csv_file_name_in = sys.argv[1]
csv_file_name_out = sys.argv[2]
csv_file_name_out_groups = sys.argv[3]
csc_code = sys.argv[4]

print ("Running procedure filtering script with the following arguments:")
print("csv_file_name_in: %s" % sys.argv[1])
print("csv_file_name_out: %s" % sys.argv[2])
print("csv_file_name_out_groups: %s" % sys.argv[3])
print("csc_code: %s" % sys.argv[4])

filter_procedures_csv(csv_file_name_in, csv_file_name_out, csc_code)
generate_procgroups_csv(csv_file_name_in, csv_file_name_out_groups, csc_code)

print("Filtering script complete.")

# Example usage:
# ./external_data_csv_filter.py '/Users/ingevejs/Documents/workspace/WH-GovHHS/GovHHS-Analytics-cca-policy-knowledge-extraction-service/external-data/procedure-codes/procedures.csv' '/Users/ingevejs/Documents/workspace/WH-GovHHS/GovHHS-Analytics-cca-policy-knowledge-extraction-service/external-data/procedure-codes/procedures-supplies.csv'  '/Users/ingevejs/Documents/workspace/WH-GovHHS/GovHHS-Analytics-cca-policy-knowledge-extraction-service/external-data/procedure-groups/procedures-groups-proccode-supplies.csv' 'SUPPLIES'
