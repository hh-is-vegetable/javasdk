package cn.hyperchain.sdk.common.utils;

import cn.hyperchain.sdk.fvm.types.FVMType;
import cn.hyperchain.sdk.fvm.types.FixedLengthListType;
import cn.hyperchain.sdk.fvm.types.PrimitiveType;
import cn.hyperchain.sdk.fvm.types.StructType;
import cn.hyperchain.sdk.fvm.types.UnfixedLengthListType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.oer.its.Uint8;
import org.junit.Test;


import static org.junit.Assert.*;

public class FVMAbiTest {

    @Test
    public void fromJson() throws IOException {
        InputStream inputStream1 = Thread.currentThread().getContextClassLoader().getResourceAsStream("fvm-contract/set_hash/contract.json");
        String abiStr = FileUtil.readFile(inputStream1);
        FVMAbi abi = FVMAbi.fromJson(abiStr);
//        System.out.println(abi.getMethods().get(0).input.get(0).ty);
    }

    @Test
    public void abiCppTest() {
        {
            String payload = new FVMBuilderParam("set_hash").build();
            System.out.println(payload);
        }

        {
            System.out.println("normal string test");
            String payload = new FVMBuilderParam("set_hash")
                    .addParam(PrimitiveType.getPrimitiveType("String"), "key")
                    .addParam(PrimitiveType.getPrimitiveType("String"), "value")
                    .build();
            // List<FVMType> list1 = new ArrayList<>(Arrays.asList(
            //     PrimitiveType.getPrimitiveType("String"),
            //     PrimitiveType.getPrimitiveType("String")
            // ));
            // FuncParams params = new FuncParams();
            // params.addParams("key");
            // params.addParams("value");
            // byte[] bts = FVMAbi.encodeRaw("set_hash", list1, params.getParams());
            System.out.println(payload);
            byte[] exceptBt = new byte[]{32, 115, 101, 116, 95, 104, 97, 115, 104, 12, 107, 101, 121, 20, 118, 97, 108, 117, 101};
            System.out.println(ByteUtil.toHex(exceptBt));
        }
        System.out.println();
        {
            System.out.println("struct test");
            String payload = new FVMBuilderParam("set_hash")
                    .addParam(
                            new StructType(new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("i8"),
                                    PrimitiveType.getPrimitiveType("u64")
                            ))),
                            new ArrayList<>(
                                    Arrays.asList(3, 32)
                            ))
                    .build();
            System.out.println(payload);
            byte[] expt = new byte[]{32,103,101,116,95,104,97,115,104,3,32,0,0,0,0,0,0,0};
            System.out.println(ByteUtil.toHex(expt));
        }
        System.out.println();
        {
            System.out.println("fixed length list type");
            FVMType tp = new FixedLengthListType(PrimitiveType.getPrimitiveType("u64"), 5);
            ArrayList args = new ArrayList<>(Arrays.asList(1,2,3,4,5));
            String payload = new FVMBuilderParam("set_hash")
                    .addParam(tp, args)
                    .build();
            System.out.println(payload);
        }
        System.out.println();
        {
            System.out.println("Unfixed length list type");
            String payload = new FVMBuilderParam("set_hash")
                    .addParam(
                            new UnfixedLengthListType(PrimitiveType.getPrimitiveType("u32")),
                            new ArrayList<>(Arrays.asList(1,2,3))
                    ).build();
            System.out.println(payload);
        }
        System.out.println();
        {
            System.out.println("set_hash(u64)");
            List<FVMType> list1 = new ArrayList<>(Arrays.asList(
                    PrimitiveType.getPrimitiveType("u64")
            ));
            FuncParams params = new FuncParams();
            params.addParams(2344);
            byte[] bts = FVMAbi.encodeRaw("set_hash", list1, params.getParams());
            System.out.println(ByteUtil.toHex(bts));
        }
        System.out.println();
        {
            System.out.println("decode test");
            String retStr = "2809000000000000"; // 2344
            Object obj = FVMAbi.decodeRaw(retStr, new ArrayList<>(Arrays.asList(
                    PrimitiveType.getPrimitiveType("u64")
            )));
            System.out.println(obj);
        }
    }
}