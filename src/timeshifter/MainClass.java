package timeshifter;

import javassist.*;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;

public class MainClass {

    public static volatile File CONF_FILE;
    public static final String FORMAT = "dd.MM.yyyy HH:mm:ss";
    public static boolean verbose = true;

    public static void premain(String args, Instrumentation inst) throws Exception {
        if (args != null && args.length() > 0) {
            System.out.println("Timeshifter: Using config from args" + args);
            CONF_FILE = new File(args);
        } else {
            System.out.println("Timeshifter: No arguments provided!");
            return;
        }

        inst.addTransformer(new Transformer());
        System.out.println("Timeshifter: Transformer added");

        tryToReload(inst);
    }

    private static void tryToReload(Instrumentation inst) {
        Class<?>[] loadedClasses = inst.getAllLoadedClasses();
        if (verbose) {
            System.out.println("Timeshifter: Already loaded " + loadedClasses.length + " classes");
        }
        ArrayList<Class<?>> classList = new ArrayList<>(loadedClasses.length);
        for (Class<?> loadedClass : loadedClasses) {
            if (inst.isModifiableClass(loadedClass)) {
                classList.add(loadedClass);
            }
        }
        Class<?>[] toRetransform = new Class<?>[classList.size()];
        classList.toArray(toRetransform);
        try {
            inst.retransformClasses(toRetransform);
        } catch (UnmodifiableClassException e) {
            System.out.println("Timeshifter: AllocationInstrumenter was unable to retransform early loaded classes.");
        } catch (UnsupportedOperationException e) {
            System.out.println("Timeshifter: Retransform is not supported on current jvm");
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
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println("Timeshifter: agentmain called");
        premain(args, inst);
    }

    private static class Transformer implements ClassFileTransformer {
        private static final ClassPool pool = ClassPool.getDefault();
        private static volatile CodeConverter codeConverter;
        static {
            try {
                CtClass jSystem = pool.get("java.lang.System");
                CtMethod jCurrentTimeMillis = jSystem.getDeclaredMethod("currentTimeMillis");
                CtMethod jCurrentTimeNanos = jSystem.getDeclaredMethod("nanoTime");
                CtClass mySystem = pool.get("timeshifter.ShiftedTimeSystem");
                CtMethod myCurrentTimeMillis = mySystem.getDeclaredMethod("currentTimeMillis");
                CtMethod myCurrentTimeNanos = mySystem.getDeclaredMethod("nanoTime");

                codeConverter = new CodeConverter();
                codeConverter.redirectMethodCall(jCurrentTimeMillis, myCurrentTimeMillis);
                codeConverter.redirectMethodCall(jCurrentTimeNanos, myCurrentTimeNanos);
            } catch (Exception e) {
                System.out.println("Timeshifter: Couldn't replace System.currentTimeMillis(): ");
                e.printStackTrace();
            }
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (!needToBeTransformed(className)) {
                return null;
            }
            try {
                CtClass clazz = pool.makeClass(className);
                if (clazz.isFrozen()) {
                    return null;
                }
                instrumentClass(clazz);
                classfileBuffer = clazz.toBytecode();
                if (verbose) {
                    System.out.println("Timeshifter: Transformed class: " + clazz.getName());
                }
            } catch (Exception e) {
                System.out.println("Timeshifter: Couldn't replace System.currentTimeMillis(): ");
                e.printStackTrace();
            }
            return classfileBuffer;
        }

        private static void instrumentClass(CtClass clazz) throws CannotCompileException {
            CtConstructor[] constructors = clazz.getConstructors();
            for (CtConstructor constructor : constructors) {
                constructor.instrument(codeConverter);
            }
            CtMethod[] methods = clazz.getDeclaredMethods();
            for (CtMethod method : methods) {
                method.instrument(codeConverter);
            }
            CtConstructor initializer = clazz.getClassInitializer();
            if (initializer != null) {
                initializer.instrument(codeConverter);
            }
//                CtClass[] innerClasses = clazz.getNestedClasses();
//                for (CtClass inner : innerClasses) {
//                    transform(loader, inner.getName(), inner.getClass(),
//                            protectionDomain, inner.toBytecode());
//                }
            // todo: inner classes?
        }

        private static boolean needToBeTransformed(String className) {
            return !className.contains("timeshifter");
        }
    }
}
