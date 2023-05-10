package com.alaindef.brunner

object MainPID {
    /**
     * @param args Any arguments passed from stdin
     */
    fun main(args: Array<String?>?) {
        val miniPID: MiniPID
        miniPID = MiniPID(0.25f, 0.01f, 0.4f)
        miniPID.setOutputLimits(10.0f)
        //miniPID.setMaxIOutput(2);
        //miniPID.setOutputRampRate(3);
        //miniPID.setOutputFilter(.3);
        miniPID.setSetpointRange(40.0f)
        var target = 100.0f
        var actual = 0.0f
        var output = 0.0f
        miniPID.setSetpoint(0.0f)
        miniPID.setSetpoint(target)
//        adf
//        System.err.printf("Target\tActual\tOutput\tError\n")
//        adf
        //System.err.printf("Output\tP\tI\tD\n");

        // Position based test code
        for (i in 0..99) {

            //if(i==50)miniPID.setI(.05);
            if (i == 60) target = 50.0f

            //if(i==75)target=(100);
            //if(i>50 && i%4==0)target=target+(Math.random()-.5)*50;
            output = miniPID.getOutput(actual, target)
            actual = actual + output

            //System.out.println("=========================="); 
            //System.out.printf("Current: %3.2f , Actual: %3.2f, Error: %3.2f\n",actual, output, (target-actual));
//        adf
//            System.err.printf(
//                "%3.2f\t%3.2f\t%3.2f\t%3.2f\n",
//                target,
//                actual,
//                output,
//                target - actual
//            )
//        adf

            //if(i>80 && i%5==0)actual+=(Math.random()-.5)*20;
        }
    }
}