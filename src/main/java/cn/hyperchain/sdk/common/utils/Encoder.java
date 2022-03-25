package cn.hyperchain.sdk.common.utils;

import cn.hyperchain.contract.BaseInvoke;
import cn.hyperchain.sdk.bvm.operate.BuiltinOperation;
import cn.hyperchain.sdk.crypto.HashUtil;
import cn.hyperchain.sdk.transaction.TxVersion;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import cn.hyperchain.sdk.bvm.operate.Operation;
import cn.hyperchain.sdk.bvm.operate.ProposalContentOperation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassLoaderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Encoder {

    private static final Logger logger = LogManager.getLogger(Encoder.class);
    public static final String DEPLOYMAGIC = "fefffbcd";
    private static final int jarLimit = 1024 * 512; // 512k
    private static final int classLimit = 1024 * 64; // 64k

    /**
     * encode deploy jar, get payload.
     *
     * @param fis FileinputStream for the given jar file
     * @return payload
     */
    // Main-Class name length (2 bytes) | Main-Class name | class length (4 bytes) | name length (2 bytes) | class | name | ...
    private static String encodeJar(InputStream fis) {
        FileOutputStream fos = null;
        File file = null;
        JarFile jarFile = null;

        try {
            file = new File("temp" + System.currentTimeMillis() + ".jar");
            fos = new FileOutputStream(file);
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();
            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            if (mainClass == null || mainClass.equals("")) {
                throw new RuntimeException("can't not get mainClass from manifest");
            }
            byte[] mainClassBytes = mainClass.getBytes(StandardCharsets.UTF_8);
            byte[] result = ByteUtil.merge(ByteUtil.shortToBytes((short) mainClassBytes.length), mainClassBytes);
            Enumeration<? extends ZipEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                ZipEntry jarEntry = entrys.nextElement();
                String jarName = jarEntry.getName();
                if (jarName.endsWith(".class")) {
                    InputStream in = jarFile.getInputStream(jarEntry);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    int readLen;
                    while ((readLen = in.read()) != -1) {
                        byteArrayOutputStream.write(readLen);
                    }
                    byte[] bs = byteArrayOutputStream.toByteArray();
                    if (bs.length > classLimit) {
                        throw new IOException("the single class content should not be larger than 64KB");
                    }
                    byte[] nameBytes = jarName.substring(0, jarName.length() - 6).getBytes(StandardCharsets.UTF_8);
                    result = ByteUtil.merge(result, ByteUtil.intToBytes(bs.length), ByteUtil.shortToBytes((short) nameBytes.length), bs, nameBytes);
                }
            }
            if (result.length > jarLimit) {
                throw new IOException("the contract jar content should not be larger than 512KB");
            }
            return DEPLOYMAGIC + ByteUtil.toHex(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (jarFile != null) {
                    jarFile.close();
                }
                if (file != null) {
                    file.delete();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.error("close stream fail, " + e.getMessage());
            }
        }
    }

    /**
     * get deploy payload.
     *
     * @param fis FileinputStream for the given jar file
     * @return payload
     */
    public static String encodeDeployJar(InputStream fis) {
        return Encoder.encodeDeployJar(fis, TxVersion.GLOBAL_TX_VERSION);
    }


    /**
     * get deploy payload.
     *
     * @param fis       FileinputStream for the given jar file
     * @param txVersion transaction txversion
     * @return payload
     */
    public static String encodeDeployJar(InputStream fis, TxVersion txVersion) {
        if (txVersion.isGreaterOrEqual(TxVersion.TxVersion30)) {
            return encodeJar(fis);
        }
        return encodeDeployBase(fis);
    }

    /**
     * encode wasm.
     *
     * @param fis InputStream
     * @return String
     */
    public static String encodeDeployWasm(InputStream fis) {
        return encodeDeployBase(fis);
    }

    /**
     * encode base.
     *
     * @param fis InputStream
     * @return String
     */
    public static String encodeDeployBase(InputStream fis) {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;

        try {
            bis = new BufferedInputStream(fis);
            baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = bis.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            byte[] buffer = baos.toByteArray();
            if (buffer.length > 1024 * 512) {
                throw new IOException("the contract jar should not be larger than 512KB");
            }

            return ByteUtil.toHex(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                logger.error("close stream fail, " + e.getMessage());
            }
        }
    }

    /**
     * get hvm invoke payload.
     *
     * @param bean invoke bean
     * @return payload
     */
    public static String encodeInvokeBeanJava(BaseInvoke bean) {
        try {
            //1. get the bean class bytes
            ClassLoaderRepository repository = new ClassLoaderRepository(Thread.currentThread().getContextClassLoader());
            JavaClass beanClass = repository.loadClass(bean.getClass());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            beanClass.dump(baos);
            byte[] clazz = baos.toByteArray();
            if (clazz.length > 0xffff) {
                throw new IOException("the bean class is too large"); // 64k
            }
            //2. get the bean class name
            byte[] clzName = bean.getClass().getCanonicalName().getBytes(Utils.DEFAULT_CHARSET);
            if (clzName.length > 0xffff) {
                throw new IOException("the bean class name is too large"); // 64k
            }
            //3. get the bin of bean
            Gson gson = new Gson();
            byte[] beanBin = gson.toJson(bean).getBytes(Utils.DEFAULT_CHARSET);
            //4. accumulate: | class length(4B) | name length(2B) | class | class name | bin |
            //               | len(txHash)      | len("__txHash__")| txHash | "__txHash__" | bin |
            StringBuilder sb = new StringBuilder();
            sb.append(ByteUtil.toHex(ByteUtil.intToByteArray(clazz.length)));
            sb.append(ByteUtil.toHex(ByteUtil.shortToBytes((short) clzName.length)));

            sb.append(ByteUtil.toHex(clazz));
            sb.append(ByteUtil.toHex(clzName));
            sb.append(ByteUtil.toHex(beanBin));
            return sb.toString();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get bvm invoke payload.
     *
     * @param methodName method name
     * @param params     invoke params
     * @return payload
     */
    public static String encodeBVM(String methodName, String... params) {
        int allLen = 0;
        allLen += 8;
        allLen += methodName.length();
        for (String param : params) {
            allLen += param.length();
            allLen += 4;
        }
        byte[] payload = new byte[allLen];

        int start = 0;
        System.arraycopy(ByteUtil.intToBytes(methodName.length()), 0, payload, start, 4);
        start += 4;
        System.arraycopy(methodName.getBytes(), 0, payload, start, methodName.getBytes().length);
        start += methodName.getBytes().length;
        System.arraycopy(ByteUtil.intToBytes(params.length), 0, payload, start, 4);
        start += 4;

        for (String param : params) {
            System.arraycopy(ByteUtil.intToBytes(param.getBytes().length), 0, payload, start, 4);
            start += 4;
            System.arraycopy(param.getBytes(), 0, payload, start, param.getBytes().length);
            start += param.getBytes().length;
        }

        return ByteUtil.toHex(payload);
    }

    /**
     * encode contract event to sha3 to get event topics.
     *
     * @param abi abi
     * @return event hash
     */
    public static HashMap<String, String> encodeEVMEvent(String abi) {
        HashMap<String, String> eventMap = new HashMap<String, String>();
        JsonArray abiArray = new JsonParser().parse(abi).getAsJsonArray();

        // get events abi prepare to decode the event
        for (int i = 0; i < abiArray.size(); i++) {
            //solve the event data in abi
            JsonObject methodBody = abiArray.get(i).getAsJsonObject();
            if (methodBody.has("name") && methodBody.get("type").getAsString().equals("event") && !methodBody.get("anonymous").getAsBoolean()) {
                String eventName = methodBody.get("name").getAsString();
                JsonArray list = methodBody.get("inputs").getAsJsonArray();
                StringBuilder sb = new StringBuilder("(");
                for (JsonElement object : list) {
                    JsonObject eventBody = object.getAsJsonObject();
                    sb.append(eventBody.get("type").getAsString()).append(",");
                }
                sb.setCharAt(sb.length() - 1, ')');
                String event = eventName + sb.toString();
                String eventId = "0x" + ByteUtil.toHex(HashUtil.sha3(event.getBytes()));
                eventMap.put(eventName, eventId);


            }
        }
        return eventMap;
    }

    /**
     * encode topic to hash hex.
     *
     * @param topic topic
     * @return hash hex
     */
    public static String encodeEventTopic(String topic) {
        byte[] data = new byte[32];
        byte[] topicData = topic.getBytes(Utils.DEFAULT_CHARSET);
        if (topicData.length > 32) {
            System.arraycopy(topicData, topicData.length - 32, data, 0, 32);
        } else {
            System.arraycopy(topicData, 0, data, 32 - topicData.length, topicData.length);
        }
        return ByteUtil.toHex(data);
    }

    /**
     * encode ProposalContentOperations.
     *
     * @param ops proposal content operations
     * @return encode byte in payload
     */
    public static byte[] encodeProposalContents(ProposalContentOperation[] ops) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // encode : |operation length(4)|operation|...
        // operation construct with : |method length(4b)|method|params count(4)|params1 length(4)|params1|...
        try {
            if (ops == null) {

                bos.write(ByteUtil.intToBytes(0));

            } else {
                bos.write(ByteUtil.intToBytes(ops.length));
            }
            for (ProposalContentOperation pco : ops) {
                bos.write(encodeOperation(pco));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }


    /**
     * encode Operation to payload.
     *
     * @param opt operation
     * @return payload
     */
    public static byte[] encodeOperation(Operation opt) {
        // encode : |method length(4b)|method|params count(4)|params1 length(4)|params1|...

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = opt.getMethod().getMethodName().getBytes();
        try {
            bos.write(ByteUtil.intToBytes(b.length));

            bos.write(b);
            int argLen;
            if (opt.getArgs() == null) {
                argLen = 0;
            } else {
                argLen = opt.getArgs().length;
            }
            bos.write(ByteUtil.intToBytes(argLen));

            boolean[] base64Index = null;
            if (opt instanceof BuiltinOperation) {
                BuiltinOperation bo = (BuiltinOperation) opt;
                base64Index = bo.getBase64Index();
            }
            for (int i = 0; i < argLen; i++) {
                byte[] bytes = base64Index != null && base64Index[i] ? Base64.getDecoder().decode(opt.getArgs()[i]) : opt.getArgs()[i].getBytes();
                byte[] intToBytes = ByteUtil.intToBytes(bytes.length);
                bos.write(intToBytes);
                bos.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }
}
