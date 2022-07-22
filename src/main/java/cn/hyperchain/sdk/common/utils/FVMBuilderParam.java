package cn.hyperchain.sdk.common.utils;

import java.util.ArrayList;
import java.util.List;

import cn.hyperchain.sdk.fvm.types.FVMType;

public class FVMBuilderParam {

    private String methodName;
    private List<FVMType> input;
    private List<Object> inputArgs;
    // private List<FVMType> output;

    public FVMBuilderParam() {
        this.methodName = "";
        input = new ArrayList<>();
        inputArgs = new ArrayList<>();
    }

    public FVMBuilderParam(String methodName) {
        this.methodName = methodName;
        input = new ArrayList<>();
        inputArgs = new ArrayList<>();
        // output = new ArrayList<>();
    }

    public FVMBuilderParam addParam(FVMType fvmType, Object arg) {
        this.input.add(fvmType);
        this.inputArgs.add(arg);
        return this;
    }
    // public FVMBuilderParam addReturn(FVMType fvmType) {
    // this.output.add(fvmType);
    // return this;
    // }

    public String build() {
        return ByteUtil.toHex(FVMAbi.encodeRaw(this.methodName, this.input, this.inputArgs));
    }
}
