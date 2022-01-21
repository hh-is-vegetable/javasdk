package cn.hyperchain.sdk.common.utils;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

/**
 * this class is used to build param for directly invoke hvm contract.
 *
 * @author Lam
 * @ClassName InvokeDirectParams
 * @date 2019/12/19
 */
public class InvokeDirectlyParams {
    private String params;

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public static class ParamBuilder {
        public static final String MAGIC = "fefffbce";
        private ByteArrayOutputStream payload;
        private Gson gson = new Gson();
        private InvokeDirectlyParams invokeDirectlyParams;

        /**
         * create params instance.
         *
         * @param methodName name of method you want to invoke
         */
        public ParamBuilder(String methodName) {
            try {
                invokeDirectlyParams = new InvokeDirectlyParams();
                payload = new ByteArrayOutputStream();
                payload.write(get2Length(methodName.getBytes().length));
                payload.write(methodName.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * add Integer argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addInteger(Integer arg) {
            checkParam(arg);
            String clazzName = Integer.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add int argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addint(int arg) {
            String clazzName = int.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Short argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addShort(Short arg) {
            checkParam(arg);
            String clazzName = Short.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add short argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addshort(short arg) {
            String clazzName = short.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Long argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addLong(Long arg) {
            checkParam(arg);
            String clazzName = Long.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add long argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addlong(long arg) {
            String clazzName = long.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Byte argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addByte(Byte arg) {
            checkParam(arg);
            String clazzName = Byte.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add byte argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addbyte(byte arg) {
            String clazzName = byte.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Float argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addFloat(Float arg) {
            checkParam(arg);
            String clazzName = Float.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add float argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addfloat(float arg) {
            String clazzName = float.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Double argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addDouble(Double arg) {
            checkParam(arg);
            String clazzName = Double.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add double argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder adddouble(double arg) {
            String clazzName = double.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Character argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addCharacter(Character arg) {
            checkParam(arg);
            String clazzName = Character.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add char argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addchar(char arg) {
            String clazzName = char.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Boolean argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addBoolean(Boolean arg) {
            String clazzName = Boolean.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add boolean argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addboolean(boolean arg) {
            String clazzName = boolean.class.getName();
            String param = String.valueOf(arg);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add String argument.
         *
         * @param arg the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addString(String arg) {
            checkParam(arg);
            String clazzName = String.class.getName();
            String param = arg;
            writePayload(clazzName, param);
            return this;
        }

        /**
         * add Object argument.
         *
         * @param clazz class of obj
         * @param obj   the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addObject(Class<?> clazz, Object obj) {
            if (obj.getClass() != clazz) {
                throw new RuntimeException("class name not equal with obj's type");
            }
            if (String.class == clazz) {
                return addString((String) obj);
            }

            String clazzName = clazz.getName();
            String param = gson.toJson(obj);
            writePayload(clazzName, param);
            return this;
        }

        /**
         * this method if for add generic object.
         *
         * @param classes index 0 for collection
         * @param obj     the argument you want to pass to contract method invoke
         * @return {@link ParamBuilder}
         */
        public ParamBuilder addParamizedObject(Class<?>[] classes, Object obj) {
            if (classes.length <= 1) {
                throw new RuntimeException("can not detect generic type");
            } else if (!(Map.class.isAssignableFrom(classes[0]) || Collection.class.isAssignableFrom(classes[0]))) {
                throw new RuntimeException(classes[0].getName() + " does not implements Collection or Map interface, we can only accpect class which is subclass of interface Map or Collection!");
            }

            StringBuilder paramizedTypeSB = new StringBuilder();
            int paramNum = 1;
            if (Map.class.isAssignableFrom(classes[0])) {
                paramNum += 2;
            } else {
                paramNum += 1;
            }

            paramizedTypeSB.append(classes[0].getName() + "<");
            for (int i = 1; i < paramNum; i++) {
                paramizedTypeSB.append(classes[i].getName());
                if (i < paramNum - 1) {
                    paramizedTypeSB.append(", ");
                }
            }
            paramizedTypeSB.append(">");
            String paramizedTypeName = paramizedTypeSB.toString();

            writePayload(paramizedTypeName, gson.toJson(obj));
            return this;
        }

        /**
         * build param instance.
         *
         * @return {@link ParamBuilder}
         */
        public InvokeDirectlyParams build() {
            invokeDirectlyParams.setParams(MAGIC + ByteUtil.toHex(payload.toByteArray()));
            return invokeDirectlyParams;
        }

        private void writePayload(String clazzName, String param) {
            try {
                payload.write(get2Length(clazzName.getBytes().length));
                payload.write(get4Length(param.getBytes().length));
                payload.write(clazzName.getBytes());
                payload.write(param.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private byte[] get2Length(int length) {
            return ByteBuffer.allocate(2).putShort((short) length).array();
        }

        private byte[] get4Length(int length) {
            return ByteBuffer.allocate(4).putInt(length).array();
        }

        private void checkParam(Object param) {
            if (param == null) {
                throw new IllegalArgumentException("the param can not be null");
            }
        }
    }

}
