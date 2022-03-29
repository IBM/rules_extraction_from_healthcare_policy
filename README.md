# Rules extraction from Healthcare policy
*Tools for protecting vital healthcare programs by extracting actionable knowledge from policy.*

### PAS Tuple extraction
Automated extraction of Predicate Argument Structure (PAS) from human annotated examples is used for genarating candidate list of tuples when extracting rules from a healthcare policy [1]. Prior to learning extraction rules and extracting tokens, a domain-specific-tokenization is applied to incoming sentences. An example of domain-specific-tokenization is demonstrated [here](spacy-extractor/src/jupyter-notebook-code/ipynb/domain-specific-tokenization.ipynb). 

The PAS tuple extraction is divided into 2 phases, which is demonstrated using the python notebooks:

- <b> Rule extraction phase: </b> This phase extracts Semgrex rules to characterize a subtree of interest. The extraction rules are extracted using human annotated examples. The pipeline for extracting the semgrex rules is automated and is demonstrated [here](spacy-extractor/src/jupyter-notebook-code/ipynb/extraction-rule-learning-phase.ipynb)
- <b> Runtime extraction: </b> This phase extracts the relevant tokens by traversing a dependency tree using Semgrex patterns extracted in previous step. The runtime extraction of tokens is demonstrated [here](spacy-extractor/src/jupyter-notebook-code/ipynb/runtime-extraction.ipynb).


For further detailed description of the above steps please review the research article [1].
### Ontology and Entity Lifting
We provide a [base ontology](ontology-and-lifting) and instructions on how to add domain-specific entities (instances) to it.

### Benefit Rules
We provide two collections of benefit rules: [1](benefit_rules_90.json), [2](benefit_rules_51.json) — that are both human-understandable and machine consumable, extracted from paragraphs from two dental policies.  Benefit Rules represent the conditions and values, defined in the ontology, that with domain experts curation and oversight can be used to flag discrepancies from policy in insurance claims. These ground truth Benefit Rules are used solely for the purpose of benchmarking the ontology-guided Benefit Rule extraction experiments presented in the paper [1]. 

### References
1. Towards protecting vital healthcare programs by extracting actionable knowledge from policy. Vanessa Lopez, Nagesh Yadav, Gabriele Picco, Inge Vejsbjerg, Eoin Carroll, Seamus Brady, Marco Luca Sbodio, Lam Thanh Hoang, Miao Wei, John Segrave. Findings of ACL, 2021.
2.  Brisimi, Theodora & Lopez, Vanessa & Rho, Valentina & Sbodio, Marco & Picco, Gabriele & Kristiansen, Morten & Segrave-Daly, John & Cullen, Conor. (2020). Ontology-guided Policy Information Extraction for Healthcare Fraud Detection. MIE 2020 
3.  Benefit graph extraction from healthcare policies. Vanessa Lopez, Valentina Rho, Theodora Brisimi et al. ISWC, 2019

