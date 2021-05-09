import instrumentation.BytesAndName;
import instrumentation.PreTransformer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InjectClassTest {

    public static final String OUTER_NAME = "io.opentelemetry.javaagent.StaticInstrumenter";
    public static final String JAR_PATH = System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar";

    @Test
    public void injectInnerClass() throws IOException {

        TypeDescription sampleInner = new TypeDescription.Latent(OUTER_NAME+"$Inner", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OBJECT) {
            @Override
            public String getSimpleName() {
                return "Inner";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return true;
            }
        };

        DynamicType.Unloaded<?> outerClazz = new ByteBuddy()
                .subclass(Object.class)
                .name(OUTER_NAME)
                .declaredTypes(sampleInner)
                .make();

        Class<?> outer = outerClazz.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened()).getLoaded();

        DynamicType.Unloaded<?> innerClazz = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .subclass(Object.class)
                .name(sampleInner.getName())
//                .modifiers(Modifier.STATIC)
                .innerTypeOf(outer).asMemberType()
//                .nestMembers(outer).modifiers(Modifier.STATIC)
                .make();

        Class<?> type = innerClazz.load(outer.getClassLoader()).getLoaded();

//        try{
//
            innerClazz.saveIn(new File("target/generated-sources"));
            outerClazz.saveIn(new File("target/generated-sources"));
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
        assertTrue(type.isMemberClass());
//        assertFalse(List.of(type.getFields()).isEmpty());

    }


    public static DynamicType.Builder<Object> getBasicType(String name){
        return new ByteBuddy().subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS).name(name);
    }
    @Test
    public void injectBytesAndName() throws IOException, NoSuchMethodException {


        Class<?> bytesAndName = BytesAndName.class;

        var classDef = getBasicType(bytesAndName.getName());


        var defaultConstructor = MethodCall.invoke(Object.class.getConstructor());
        Implementation.Composable constructorInvocation = null;
        int i = 0;
        for(var field : bytesAndName.getFields()){

            classDef = classDef
                    .defineField(
                            field.getName(),
                            field.getType(),
                            field.getModifiers()
                    );

            if(i == 0) constructorInvocation = defaultConstructor.andThen(FieldAccessor.ofField(field.getName()).setsArgumentAt(i));
            else constructorInvocation =  constructorInvocation.andThen(FieldAccessor.ofField(field.getName()).setsArgumentAt(i));
            i++;
        }




        Class<?>[] parametersArr = Arrays.stream(bytesAndName.getFields()).map(Field::getType).toArray(Class<?>[]::new);
        Constructor<?> constructor = bytesAndName.getConstructor(parametersArr);

        classDef = classDef
                .defineConstructor(
                        constructor.getModifiers()
                )
                .withParameters(
                        parametersArr
                )
                .intercept(
                        constructorInvocation
                );


        classDef.make().saveIn(new File("target/generated-sources"));

    }

    @Test
    public void createTransformer() throws IOException {

        Class<?> preTransformerClass = PreTransformer.class;
        var classDef = getBasicType(preTransformerClass.getName());


        Class<?>[] interfaces = preTransformerClass.getInterfaces();

        for(var interf : interfaces){
            classDef = classDef.implement(interf);
        }

        for(var method : preTransformerClass.getMethods())

        classDef.make().saveIn(new File("target/generated-sources"));

//        for(var method : preTransformerClass.getMethods()){
//            classDef.defineMethod(
//                    method.getName(),
//                    method.getReturnType(),
//                    method.get
//            )
//        }

//        classDef.implement(Trans)

    }

    @Test
    public void testCleanRebase() throws IOException {
        new ByteBuddy()
                .rebase(BytesAndName.class)
                .make()
                .saveIn(new File("target/generated-sources"));

        new ByteBuddy()
                .rebase(PreTransformer.class)
                .make()
                .saveIn(new File("target/generated-sources"));

    }

}
