package team.chisel.client.render.texture;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Point2i;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import team.chisel.api.render.IBlockRenderContext;
import team.chisel.api.render.TextureInfo;
import team.chisel.client.render.Quad;
import team.chisel.client.render.ctm.ISubmap;
import team.chisel.client.render.ctm.Submap;
import team.chisel.client.render.ctx.BlockRenderContextPattern;
import team.chisel.client.render.ctx.BlockRenderContextPosition;
import team.chisel.client.render.type.BlockRenderTypeMap;

public class ChiselTextureMap extends AbstractChiselTexture<BlockRenderTypeMap> {

    public enum MapType {
        RANDOM {

            @Override
            protected List<BakedQuad> transformQuad(ChiselTextureMap tex, BakedQuad quad, @Nullable IBlockRenderContext context, int quadGoal) {
                EnumFacing side = quad.getFace();

                BlockPos pos = context == null ? new BlockPos(0, 0, 0) : ((BlockRenderContextPosition) context).getPosition();
                rand.setSeed(MathHelper.getPositionRandom(pos) + side.ordinal());
                rand.nextBoolean();

                int w = tex.xSize;
                int h = tex.ySize;
                float intervalX = 16f / w;
                float intervalY = 16f / h;

                int unitsAcross = rand.nextInt(w) + 1;
                int unitsDown = rand.nextInt(h) + 1;

                float maxU = unitsAcross * intervalX;
                float maxV = unitsDown * intervalY;
                ISubmap uvs = new Submap(intervalX, intervalY, maxU - intervalX, maxV - intervalY);

                Quad q = Quad.from(quad).setFullbright(tex.fullbright);
                
                // TODO move this code somewhere else, it's copied from below
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0].getSprite(), uvs).setFullbright(tex.fullbright).rebake());
                } else {
                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0].getSprite(), uvs);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).collect(Collectors.toList());
                }
            }
        },
        PATTERNED {

            @Override
            protected List<BakedQuad> transformQuad(ChiselTextureMap tex, BakedQuad quad, @Nullable IBlockRenderContext context, int quadGoal) {
                
                Point2i textureCoords = context == null ? new Point2i(0, 0) : ((BlockRenderContextPattern)context).getTextureCoords(quad.getFace());
                
                float intervalU = 16f / tex.xSize;
                float intervalV = 16f / tex.ySize;

                // throw new RuntimeException(index % variationSize+" and "+index/variationSize);
                float minU = intervalU * textureCoords.x;
                float minV = intervalV * textureCoords.y;

                ISubmap submap = new Submap(intervalU, intervalV, minU, minV);

                Quad q = Quad.from(quad).setFullbright(tex.fullbright);
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0].getSprite(), submap).rebake());
                } else {
                    // Chisel.debug("V texture complying with quad goal of 4");
                    // Chisel.debug(new float[] { minU, minV, minU + intervalU, minV + intervalV });

                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0].getSprite(), submap);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).collect(Collectors.toList());
                }
            }
            
            @Override
            public IBlockRenderContext getContext(@Nonnull BlockPos pos, @Nonnull ChiselTextureMap tex) {
                return new BlockRenderContextPattern(pos, tex).applyOffset();
            }
        };

        protected abstract List<BakedQuad> transformQuad(ChiselTextureMap tex, BakedQuad quad, @Nullable IBlockRenderContext context, int quadGoal);
        
        public IBlockRenderContext getContext(@Nonnull BlockPos pos, @Nonnull ChiselTextureMap tex) {
            return new BlockRenderContextPosition(pos);
        }
    }

    @Getter
    private final int xSize;
    @Getter
    private final int ySize;

    private final MapType map;

    private static final Random rand = new Random();

    public ChiselTextureMap(BlockRenderTypeMap type, TextureInfo info, MapType map) {
        super(type, info);

        this.map = map;

        if (this.info.isPresent()) {
            JsonObject object = this.info.get();
            if (object.has("width") && object.has("height")) {
                Preconditions.checkArgument(object.get("width").isJsonPrimitive() && object.get("width").getAsJsonPrimitive().isNumber(), "width must be a number!");
                Preconditions.checkArgument(object.get("height").isJsonPrimitive() && object.get("height").getAsJsonPrimitive().isNumber(), "height must be a number!");

                this.xSize = object.get("width").getAsInt();
                this.ySize = object.get("height").getAsInt();

            } else if (object.has("size")) {
                Preconditions.checkArgument(object.get("size").isJsonPrimitive() && object.get("size").getAsJsonPrimitive().isNumber(), "size must be a number!");

                this.xSize = object.get("size").getAsInt();
                this.ySize = object.get("size").getAsInt();
            } else {
                xSize = ySize = 2;
            }
        } else {
            xSize = ySize = 2;
        }

        Preconditions.checkArgument(xSize > 0 && ySize > 0, "Cannot have a dimension of 0!");
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, IBlockRenderContext context, int quadGoal) {
        return map.transformQuad(this, quad, context, quadGoal);
    }
}
