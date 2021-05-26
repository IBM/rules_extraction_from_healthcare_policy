# Rules extraction from Healthcare policy
*Tools for protecting vital healthcare programs by extracting actionable knowledge from policy.*

### Ontology and Entity Lifting
We provide a [base ontology](ontology-and-lifting) and instructions on how to add domain-specific entities (instances) to it.

### PAS Tuple extraction
[Jupyter notebooks](spacy-extractor) showcasing the Predicate Argument Structure (PAS) tuple extraction implementation from policy sentences with the learned rules.

### Benefit Rules
We provide a [collection of 'benefit rules'](benefit_rules.json) that are both human-understandable and machine consumable, extracted from paragraphs from two dental policies.  Benefit Rules represent the conditions and values, defined in the ontology, that with domain experts curation and oversight can be used to flag discrepancies from policy in insurance claims. These ground truth Benefit Rules are used solely for the purpose of benchmarking the ontology-guided Benefit Rule extraction experiments presented in the paper: 
Towards protecting vital healthcare programs by extracting actionable knowledge from policy. Vanessa Lopez, Nagesh Yadav, Gabriele Picco, Inge Vejsbjerg, Eoin Carrol, Seamus Brady, Marco Luca Sbodio,  Lam Thanh Hoang, Miao Wei, John Segrave. Findings of ACL, 2021

