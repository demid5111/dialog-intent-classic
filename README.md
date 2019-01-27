## Info

This is the fork of [this repo][source-repo].
The main diff is usage of gradle build and cleaned up structure.

## How to setup

1. Download the library spmf.jar from [official website][spmf-source] into the `libs` folder in the root.

[spmf-source]: http://www.philippe-fournier-viger.com/spmf/index.php?link=download.php
[source-repo]: https://github.com/numiareced/sequentials

## Running examples

For example, you can setup the Intellij IDEA and make the following configuration:

```sh
-PappArgs="['-algos','TDAG,DG,AKOM,CPT,CPTPlus,PPM,LZ78', '-general', 'False', '-o',
'full_path_to_project_root/out', '-i', 'full_path_to_the_folder_with_intentions_dataset/', 
'-number-runs', '100', '-max-number-files', '-1', '-min-seqence-length','5', '-training-ratio','0.8']"
```