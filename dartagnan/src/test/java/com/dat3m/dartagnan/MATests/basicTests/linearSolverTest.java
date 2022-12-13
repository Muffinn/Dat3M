package com.dat3m.dartagnan.MATests.basicTests;

import com.dat3m.dartagnan.c.AbstractCTest;
import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.verification.solving.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.LinkedList;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class linearSolverTest extends AbstractCTest {

    private String reportFileName;

    public linearSolverTest(String name, Arch target, Result expected, String reportFileName) {
        super(name, target, expected);
        this.reportFileName = reportFileName;
    }

    @Override
    protected Provider<String> getProgramPathProvider() {
        return Provider.fromSupplier(() -> TEST_RESOURCE_PATH + "lfds/" + name + ".bpl");
    }

    @Override
    protected long getTimeout() {
        return 600000;
    }

    protected Provider<Integer> getBoundProvider() {
        return Provider.fromSupplier(() -> 2);
    }

	@Parameterized.Parameters(name = "{index}: {0}, target={1}")
    public static Iterable<Object[]> data() throws IOException {
		Object[] jitDelete = new Object[]{"treiber-3", C11, UNKNOWN, "JIT_Delete"};
        LinkedList<Object[]> objectList = new LinkedList<Object[]>();
        objectList.add(jitDelete);
        String testName = "LinearSolvers";

        String[] programNames = {"dglm-3", "ms-3", "treiber-3"};
        Arch[] wmms = {TSO, ARM8, C11};
        for (Arch wmm:wmms) {
            for (String programName : programNames) {
                for (int j = 0; j < 5; j++) {
                    Object[] objects = new Object[]{programName, wmm, UNKNOWN, testName};
                    objectList.add(objects);
                }
            }
        }

        return objectList;
    }

	@Test
	@CSVLogger.FileName("csv/assume")
	public void testAssume() throws Exception {

        long startTime = System.currentTimeMillis();
        AssumeSolver s = AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        long endTime = System.currentTimeMillis();
        Calendar date = Calendar.getInstance();

        String reportString = new String();
        reportString += reportFileName + ",";
        reportString += "ASolver,";
        reportString += name + ",";
        reportString += target.name() + ",";
        reportString +=date.get(Calendar.DAY_OF_MONTH) + ".";
        reportString +=date.get(Calendar.MONTH) + ".";
        reportString +=date.get(Calendar.YEAR) + ",";
        reportString += startTime + ",";
        reportString += endTime;

        String reportFileNameS = reportFileName + "_" + "ASolver" + "_"
                + name + "_" + target.name();
        String fullName = "output/reports/" + reportFileNameS + ".csv";


        try(FileWriter fileWriter = new FileWriter(fullName, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);) {
            printWriter.println(reportString);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }



        assertEquals(expected, s.getResult());
	}

	@Test
	@CSVLogger.FileName("csv/refinement")
	public void testRefinement() throws Exception {
        long startTime = System.currentTimeMillis();
        RefinementSolver s = RefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        ExecutionModel ec = ExecutionModel.withContext(s.getEncodingContext());
        ec.initialize(proverProvider.get().getModel());
        long endTime = System.currentTimeMillis();
        Calendar date = Calendar.getInstance();

        String reportString = new String();
        reportString += reportFileName + ",";
        reportString += "RSolver,";
        reportString += name + ",";
        reportString += target.name() + ",";
        reportString +=date.get(Calendar.DAY_OF_MONTH) + ".";
        reportString +=date.get(Calendar.MONTH) + ".";
        reportString +=date.get(Calendar.YEAR) + ",";
        reportString += startTime + ",";
        reportString += endTime;

        String reportFileNameS = reportFileName + "_" + "RSolver" + "_"
                + name + "_" + target.name();
        String fullName = "output/reports/" + reportFileNameS + ".csv";


        try(FileWriter fileWriter = new FileWriter(fullName, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);) {
            printWriter.println(reportString);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        assertEquals(expected, s.getResult());
	}






}