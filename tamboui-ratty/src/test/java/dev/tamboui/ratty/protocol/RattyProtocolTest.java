/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import dev.tamboui.ratty.ObjectFormat;
import dev.tamboui.ratty.PlaceOptions;
import dev.tamboui.ratty.UpdateOptions;

import static org.assertj.core.api.Assertions.assertThat;

class RattyProtocolTest {

    private static final String APC = "\u001b_";
    private static final String ST = "\u001b\\";

    @Test
    void supportQuery() {
        String command = RattyProtocol.supportQuery();
        assertThat(command).isEqualTo(APC + "ratty;g;s" + ST);
    }

    @Test
    void registerByPath() {
        String command = RattyProtocol.registerByPath(42, ObjectFormat.OBJ, "model.obj");
        assertThat(command).isEqualTo(APC + "ratty;g;r;id=42;fmt=obj;path=model.obj" + ST);
    }

    @Test
    void registerByPath_glb() {
        String command = RattyProtocol.registerByPath(7, ObjectFormat.GLB, "scene.glb");
        assertThat(command).isEqualTo(APC + "ratty;g;r;id=7;fmt=glb;path=scene.glb" + ST);
    }

    @Test
    void registerByPayload() {
        byte[] data = "fake obj data".getBytes(StandardCharsets.UTF_8);
        String command = RattyProtocol.registerByPayload(10, ObjectFormat.OBJ, data, "test.obj");
        String expectedBase64 = Base64.getEncoder().encodeToString(data);
        assertThat(command).isEqualTo(APC + "ratty;g;r;id=10;fmt=obj;source=payload;more=0;name=test.obj;" + expectedBase64 + ST);
    }

    @Test
    void registerByPayload_noFilename() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        String command = RattyProtocol.registerByPayload(10, ObjectFormat.OBJ, data, null);
        String expectedBase64 = Base64.getEncoder().encodeToString(data);
        assertThat(command).isEqualTo(APC + "ratty;g;r;id=10;fmt=obj;source=payload;more=0;" + expectedBase64 + ST);
    }

    @Test
    void registerByPayloadChunked() {
        // Create data large enough to require chunking
        byte[] data = new byte[RattyProtocol.CHUNK_SIZE * 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        String[] commands = RattyProtocol.registerByPayloadChunked(99, ObjectFormat.GLB, data, "large.glb");
        
        // Should have multiple chunks
        assertThat(commands.length).isGreaterThan(1);

        // First chunk should have more=1 and include filename
        assertThat(commands[0]).contains("more=1");
        assertThat(commands[0]).contains("name=large.glb");

        // Last chunk should have more=0
        assertThat(commands[commands.length - 1]).contains("more=0");

        // All chunks should have proper structure
        for (String cmd : commands) {
            assertThat(cmd).startsWith(APC + "ratty;g;r;id=99;fmt=glb;source=payload");
            assertThat(cmd).endsWith(ST);
        }
    }

    @Test
    void place_minimal() {
        PlaceOptions options = PlaceOptions.builder(10, 20, 5, 3).build();
        String command = RattyProtocol.place(1, options);
        assertThat(command).isEqualTo(APC + "ratty;g;p;id=1;row=10;col=20;w=5;h=3" + ST);
    }

    @Test
    void place_withAllOptions() {
        PlaceOptions options = PlaceOptions.builder(12, 8, 4, 2)
            .animate(true)
            .scale(1.5f)
            .depth(2.5f)
            .color("ff8844")
            .brightness(1.2f)
            .translate(0.1f, 0.2f, 0.3f)
            .rotate(10f, 45f, 90f)
            .scaleNonUniform(1.0f, 1.5f, 0.8f)
            .build();

        String command = RattyProtocol.place(42, options);
        
        assertThat(command).contains("id=42");
        assertThat(command).contains("row=12");
        assertThat(command).contains("col=8");
        assertThat(command).contains("w=4");
        assertThat(command).contains("h=2");
        assertThat(command).contains("animate=1");
        assertThat(command).contains("scale=1.5");
        assertThat(command).contains("depth=2.5");
        assertThat(command).contains("color=ff8844");
        assertThat(command).contains("brightness=1.2");
        assertThat(command).contains("px=0.1");
        assertThat(command).contains("py=0.2");
        assertThat(command).contains("pz=0.3");
        assertThat(command).contains("rx=10.0");
        assertThat(command).contains("ry=45.0");
        assertThat(command).contains("rz=90.0");
        assertThat(command).contains("sx=1.0");
        assertThat(command).contains("sy=1.5");
        assertThat(command).contains("sz=0.8");
    }

    @Test
    void update_rotationOnly() {
        UpdateOptions options = UpdateOptions.builder()
            .rotateY(120f)
            .build();

        String command = RattyProtocol.update(7, options);
        
        assertThat(command).contains("id=7");
        assertThat(command).contains("ry=120.0");
        assertThat(command).doesNotContain("animate");
        assertThat(command).doesNotContain("scale=");
    }

    @Test
    void update_multipleFields() {
        UpdateOptions options = UpdateOptions.builder()
            .animate(false)
            .rotateY(180f)
            .translate(0.5f, 0.0f, 0.0f)
            .build();

        String command = RattyProtocol.update(7, options);
        
        assertThat(command).contains("animate=0");
        assertThat(command).contains("ry=180.0");
        assertThat(command).contains("px=0.5");
    }

    @Test
    void delete() {
        String command = RattyProtocol.delete(42);
        assertThat(command).isEqualTo(APC + "ratty;g;d;id=42" + ST);
    }

    @Test
    void deleteAll() {
        String command = RattyProtocol.deleteAll();
        assertThat(command).isEqualTo(APC + "ratty;g;d" + ST);
    }

    @Test
    void parseSupportResponse() {
        String response = APC + "ratty;g;s;v=1;fmt=obj|glb;path=1;payload=1;chunk=1;anim=1;depth=1;color=1;brightness=1;transform=1;update=1" + ST;
        
        RattyCapabilities caps = RattyProtocol.parseSupportResponse(response);
        
        assertThat(caps).isNotNull();
        assertThat(caps.version()).isEqualTo(1);
        assertThat(caps.formats()).containsExactlyInAnyOrder("obj", "glb");
        assertThat(caps.pathSupported()).isTrue();
        assertThat(caps.payloadSupported()).isTrue();
        assertThat(caps.chunkSupported()).isTrue();
        assertThat(caps.animSupported()).isTrue();
        assertThat(caps.depthSupported()).isTrue();
        assertThat(caps.colorSupported()).isTrue();
        assertThat(caps.brightnessSupported()).isTrue();
        assertThat(caps.transformSupported()).isTrue();
        assertThat(caps.updateSupported()).isTrue();
    }

    @Test
    void parseSupportResponse_partialCapabilities() {
        String response = APC + "ratty;g;s;v=1;fmt=obj;path=1;payload=0;anim=1" + ST;
        
        RattyCapabilities caps = RattyProtocol.parseSupportResponse(response);
        
        assertThat(caps).isNotNull();
        assertThat(caps.version()).isEqualTo(1);
        assertThat(caps.formats()).containsExactly("obj");
        assertThat(caps.pathSupported()).isTrue();
        assertThat(caps.payloadSupported()).isFalse();
        assertThat(caps.animSupported()).isTrue();
        assertThat(caps.depthSupported()).isFalse();
    }

    @Test
    void parseSupportResponse_invalidFormat() {
        assertThat(RattyProtocol.parseSupportResponse(null)).isNull();
        assertThat(RattyProtocol.parseSupportResponse("invalid")).isNull();
        assertThat(RattyProtocol.parseSupportResponse(APC + "other" + ST)).isNull();
    }
}
