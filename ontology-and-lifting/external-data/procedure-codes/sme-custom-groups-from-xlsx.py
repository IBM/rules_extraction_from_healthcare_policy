#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Utility function to convert SME provided data groups spreadsheet to a csv for ingestion by policy knowledge extraction API.
A sample of the spreadsheet format provided by Morten is available /docs/samples/sample-custom-groups.xlsx
"""
import sys
import csv
import pandas as pd
import numpy as np
from re import sub


def snake(s):
    s = str(s)
    s = s.replace(",", "_")
    s = s.replace("/", "_")
    s = '_'.join(
        sub('([A-Z][a-z]+)', r' \1',
            sub('([A-Z]+)', r' \1',
                s.replace('-', ' '))).split()).lower()
    return 'http://claims-audit.ibm.com/custom_procedure_codes_group_' + s


def strip(s):
    s = str(s).strip()
    return s


def split(s):
    s = '['+str(s).replace(' ','')+']'
    return s


def convert(csv_file_name_in, csv_file_name_out):
    df = pd.read_excel(xlsx_file_name_in, sheet_name='dme-custom-groups', engine='openpyxl')
    df = df[df["Custom Group Name - Current (0.1.2)"].notnull()]
    df = df.rename(columns={'Contained Codes': 'members'})
    df['displayName'] = np.where(~df['Custom Group Name - New '].isnull(), df['Custom Group Name - New '].apply(strip), df['Custom Group Name - Current (0.1.2)'].apply(strip))
    df['description'] = df['displayName']
    df['entityId'] = df['displayName'].apply(snake)
    df['namespace'] = 'http://claims-audit.ibm.com/'
    df['tag'] = 'procedure-codes-supplies.20210113'
    df['typesId'] = 'http://claims-audit.ibm.com/CustomProcedureCodesGroup'
    df['members'] = np.where(~df['members'].isnull(), df['members'].apply(split), "[]")

    df2 = df[['description','displayName','entityId','members','namespace','tag','typesId']]
    df2.to_csv(csv_file_name_out, index=False, header=True, quoting=csv.QUOTE_ALL)


xlsx_file_name_in = sys.argv[1]
csv_file_name_out = sys.argv[2]

print ("Running custom group conversion to csv with the following arguments:")
print("xlsx_file_name_in: %s" % sys.argv[1])
print("csv_file_name_out: %s" % sys.argv[2])
convert(xlsx_file_name_in, csv_file_name_out)
print("Conversion script complete.")


# Example usage:
# ./sme-custom-groups-from-xlsx.py '/Users/ingevejs/Downloads/DME-custom-groups.xlsx' '/Users/ingevejs/Documents/workspace/WH-GovHHS/GovHHS-Analytics-cca-policy-knowledge-extraction-service/external-data/procedures-groups/dme-supplies-sme-groups.csv'
