package timeshifter;

import javassist.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;

/**
 * some sources are copied from ru.javaorca and boiler
 */
public class MainClass {

    public static volatile File FILE;
    public static final String FORMAT = "dd.MM.yyyy HH:mm:ss";
    public static boolean verbose;

//    private final static Logger log =
//            LoggerFactory.getLogger(MainClass.class);

    public static void premain(String args, Instrumentation inst)
            throws Exception {
        if (args != null && args.length() > 0) {
//            log.info("Using {} config from args", args);
            System.out.println("Timeshifter: Using config from args" + args);
            FILE = new File(args);
        } else {
//            log.error("No arguments provided!");
            System.out.println("Timeshifter: No arguments provided!");
            return;
        }

        inst.addTransformer(new Transformer());
//        log.info("Transformer added");
        System.out.println("Timeshifter: Transformer added");

        Class<?>[] loadedClasses = inst.getAllLoadedClasses();
//        log.info("Already loaded {} classes", loadedClasses.length);
        if (verbose) {
            System.out.println("Timeshifter: Already loaded " +
                    loadedClasses.length + " classes");
        }
        ArrayList<Class<?>> classList = new ArrayList<>(loadedClasses.length);
        for (Class<?> loadedClass : loadedClasses) {
            if (inst.isModifiableClass(loadedClass)) {
                classList.add(loadedClass);
            }
        }

        // Reload classes, if possible.
        Class<?>[] toRetransform = new Class<?>[classList.size()];
        classList.toArray(toRetransform);
        try {
            inst.retransformClasses(toRetransform);
        } catch (UnmodifiableClassException e) {
//            log.warn("AllocationInstrumenter was unable to retransform " +
//                    "early loaded classes.");
            System.out.println("Timeshifter: AllocationInstrumenter was " +
                    "unable to retransform early loaded classes.");
        } catch (UnsupportedOperationException e) {
//            log.warn("Retransform is not supported on current jvm", e);
            System.out.println("Timeshifter: Retransform is not supported " +
                    "on current jvm");
            e.printStackTrace();
        }
    }

    /**
     * JVM hook to dynamically load javaagent at runtime.
     *
     * The agent class may have an agentmain method for use when the agent is
     * started after VM startup.
     *
     * @param args
     * @param inst
     * @throws Exception
     */
    public static void agentmain(String args, Instrumentation inst)
            throws Exception {
//        log.info("agentmain called");
        System.out.println("Timeshifter: agentmain called");
        premain(args, inst);
    }

    private static class Transformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (!needToBeTransformed(className)) {
                return null;
            }
            try {
                ClassPool pool = ClassPool.getDefault();

                CtClass jSystem = pool.get("java.lang.System");
                CtMethod jCurrentTime =
                        jSystem.getDeclaredMethod("currentTimeMillis");
                CtClass mySystem = pool.get("timeshifter.ShiftedTimeSystem");
                CtMethod myCurrentTime =
                        mySystem.getDeclaredMethod("currentTimeMillis");

                CodeConverter cc = new CodeConverter();
                cc.redirectMethodCall(jCurrentTime, myCurrentTime);

                CtClass clazz = pool.makeClass(
                        new ByteArrayInputStream(classfileBuffer), false);
                if (clazz.isFrozen()) {
                    return null;
                }
                CtConstructor[] constructors = clazz.getConstructors();
                for (CtConstructor constructor : constructors) {
                    constructor.instrument(cc);
                }
                CtMethod[] methods = clazz.getDeclaredMethods();
                for (CtMethod method : methods) {
                    method.instrument(cc);
                }
                CtConstructor initializer = clazz.getClassInitializer();
                if (initializer != null) {
                    initializer.instrument(cc);
                }
//                CtClass[] innerClasses = clazz.getNestedClasses();
//                for (CtClass inner : innerClasses) {
//                    transform(loader, inner.getName(), inner.getClass(),
//                            protectionDomain, inner.toBytecode());
//                }
                // todo: inner classes?

                classfileBuffer = clazz.toBytecode();
//                log.debug("Transformed class: {}", clazz.getName());
                if (verbose) {
                    System.out.println("Timeshifter: Transformed class: " +
                            clazz.getName());
                }
            } catch (Exception e) {
//                log.error("Couldn't replace System.currentTimeMillis(): ", e);
                System.out.println("Timeshifter: Couldn't replace " +
                        "System.currentTimeMillis(): ");
                e.printStackTrace();
            }
            return classfileBuffer;
        }

        private static boolean needToBeTransformed(String className) {
            return !className.contains("Timeshifter: timeshifter");
        }
    }
}
