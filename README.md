# fix-message-recon

To execute the fix-message-recon, please open a terminal and execute the following commands:

1. git clone https://github.com/renatohaber/fix-message-recon.git
2. cd fix-message-recon
3. chmod +x gradlew
4. ./gradlew bootRun

You should see something like this output containg the path to each one of the files generated during the test, and the elapsed time for each step

2021-10-24 18:39:39.543  INFO 126587 --- [           main] c.t.i.f.components.FileGenerator         : FileGenerator : data generation : timeElapsed : 119 ms
2021-10-24 18:39:39.629  INFO 126587 --- [           main] c.t.i.f.components.FileGenerator         : FileGenerator : /tmp/executions.txt : timeElapsed : 84 ms
2021-10-24 18:39:39.782  INFO 126587 --- [           main] c.t.i.f.components.AllMsgsGenerator      : AllMsgsGenerator | FulfillGenerator : data generation : timeElapsed : 152 ms
2021-10-24 18:39:39.811  INFO 126587 --- [           main] c.t.i.f.components.AllMsgsGenerator      : AllMsgs : /tmp/AllMsgs.csv : timeElapsed : 28 ms
2021-10-24 18:39:39.839  INFO 126587 --- [           main] c.t.i.f.components.AllMsgsGenerator      : FulFill : /tmp/FulFill.txt : timeElapsed : 27 ms
2021-10-24 18:39:39.923  INFO 126587 --- [           main] c.t.i.f.components.ReconFileGenerator    : ReconFileGenerator : data generation : timeElapsed : 83 ms
2021-10-24 18:39:39.935  INFO 126587 --- [           main] c.t.i.f.components.ReconFileGenerator    : ReconFileGenerator : recon file creation : /tmp/recon.csv : timeElapsed : 11 ms
