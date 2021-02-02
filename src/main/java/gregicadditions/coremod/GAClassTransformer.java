package gregicadditions.coremod;

import gregicadditions.coremod.transform.NetworkNodeGridTransformer;
import gregicadditions.coremod.transform.PacketJEIRecipeTransformer;
import gregicadditions.coremod.transform.TileEntityControllerTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class GAClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        Transform tform;
        switch (transformedName) {
            case "appeng.core.sync.packets.PacketJEIRecipe":
                tform = PacketJEIRecipeTransformer.INSTANCE;
                break;
            case "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid":
                tform = NetworkNodeGridTransformer.INSTANCE;
                break;
            case "mcjty.xnet.blocks.controller.TileEntityController":
                tform = TileEntityControllerTransformer.INSTANCE;
                break;
            default:
                return basicClass;
        }
        System.out.println("[Gregicality] Transforming class: " + transformedName);
        return tform.transformClass(basicClass);
    }

    public interface Transform {

        byte[] transformClass(byte[] code);

    }

    public static abstract class ClassMapper implements Transform {

        @Override
        public byte[]
        transformClass(byte[] code) {
            ClassReader reader = new ClassReader(code);
            ClassWriter writer = new ClassWriter(reader, getWriteFlags());
            reader.accept(getClassMapper(writer), 0);
            return writer.toByteArray();
        }

        protected int getWriteFlags() {
            return 0;
        }

        protected abstract ClassVisitor getClassMapper(ClassVisitor downstream);

    }
}
