# Rules extraction from Healthcare policy
*Tools for protecting vital healthcare programs by extracting actionable knowledge from policy.*

### Ontology and Entity Lifting
We provide a [base ontology](ontology-and-lifting) and instructions on how to add domain-specific entities to it.

### Benefit Rules
We provide a [collection of 'benefit rules'](benefit_rules.json) that are both human-understandable and machine consumable, extracted from paragraphs from two dental policies.  Benefit Rules represent the conditions and values, defined in the ontology, that can be used to flag discrepancies from policy in insurance claims, with limited human effort. These are the ground truth Benefit Rules used solely for the purpose of benchmarking the ontology-guided Benefit Rule extraction experiments presented in the paper: 
Towards protecting vital healthcare programs by extracting actionable knowledge from policy. Vanessa Lopez, Nagesh Yadav, Gabriele Picco, Inge Vejsbjerg, Eoin Carrol, Seamus Brady, Marco Luca Sbodio,  Lam Thanh Hoang, Miao Wei, John Segrave. Findings of ACL, 2021

### PAS Tuple extraction
[Jupyter notebooks](spacy-extractor) showcasing the PAS tuple extraction implementation with the learned rules.

