# How to run aggregation and evaluation script

## Generate a uqaminers_global.txt file

The evaluation script needs a uqaminers_global.txt file which contains :


> subject_id decision_id number_of_posts

ex : 

> train_subject7370 1 13

To generate this file using the aggregate_results.py you need :

* The path of a directory containing 10 txt file, 1 per labelled uqaminers_CHUNKID.txt
* The path of the writings-per-subject-all-train.txt file

Then run :

> python aggregate_results.py -path path_directory_10txt -wsource path_to_writings-per-subject-all-train.txt

the uqaminers_global.txt file will be generated next to the 10 txt files.

## Run the evaluation script

First you need to install the python dependencies :

run:
 ./prepare_env.sh
 source .env/bin/activate

Then just type :

python erisk_eval.py -gpath path/to/risk_golden_truth.txt -ppath path/to/uqaminers_global.txt -o 5