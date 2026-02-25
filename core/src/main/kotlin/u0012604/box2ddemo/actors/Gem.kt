package u0012604.box2ddemo.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import ktx.tiled.property
import ktx.tiled.type
import u0012604.box2ddemo.box2d.Box2dConstants
import u0012604.box2ddemo.box2d.buildBasicPhysicsBodyFromSprite

internal val gemClasses = arrayOf(
    "BlueGem",
    "GreyGem",
    "PinkGem",
    "GreenGem",
    "OrangeGem",
    "YellowGem")

class Gem(
    override val world: World,
    private val animation: Animation<TextureRegion>,
    textureRegion: TextureRegion,
    initPosition: Vector2) : Actor {

        private var stateTime = 0f

    // Build the sprite, taking in consideration
    // that Box2d works in meters not pixels.
    override val sprite = Sprite(animation.getKeyFrame(0f)).apply {
        val w = 32f / Box2dConstants.PIXELS_PER_METER
        val h = 32f / Box2dConstants.PIXELS_PER_METER
        setSize(w, h)
        // 2. Set the rotation point to the center of the sprite
        setOriginCenter()

        setPosition(
            (initPosition.x / Box2dConstants.PIXELS_PER_METER) - w / 2f,
            (initPosition.y / Box2dConstants.PIXELS_PER_METER) - h / 2f
        )
    }

    // Set the Box2d physics body of this actor
    override val body: Body = buildBasicPhysicsBodyFromSprite(world, this)

    private val halfSpriteWidth = sprite.width / 2f
    private val halfSpriteHeight = sprite.height / 2f

    // ---------------------------------------------------------
    // update()
    // Apply Box2d physics to the sprite's position.
    // ---------------------------------------------------------
    override fun update() {
        stateTime += Gdx.graphics.deltaTime

        sprite.setRegion(animation.getKeyFrame(stateTime, true))

        body?.let {
            sprite.setPosition(it.position.x - sprite.width / 2f, it.position.y - sprite.height / 2f )
            sprite.rotation = it.angle * MathUtils.radDeg
        }
    }

    override fun dispose() {

    }

    companion object {
        // ---------------------------------------------------------
        // Build a collection of gems for each gem found in the map.
        // ---------------------------------------------------------
        fun buildGemCollection(world: World, map: TiledMap, texture: Texture): List<Gem> {
            val textureRegions = TextureRegion.split(texture, 32, 32)

            return  map.layers
                        .get("Objects")
                        .objects
                        .filter {
                            // Use the Gem's class name (or type in LibGDX) - see this: https://stackoverflow.com/questions/62938262/how-to-access-tiledmap-objects-type-or-a-custom-property-thats-specified-in-ti#:~:text=1%20Answer,have%20been%20overridden%20or%20not).
                            val klass = it.properties["type"] as String?
                            gemClasses.contains(klass)
                        }
                        .map {
                            val index = gemClasses.indexOf(it.properties["type"] as String)
                            val gemAnimation = Animation(0.1f, *textureRegions[index])

                            val x = it.property<Float>("x")
                            val y = it.property<Float>("y")
                            Gem(world, gemAnimation, textureRegions[index][0], Vector2(x, y))
                        }
                        .toList()
        }
    }
}
