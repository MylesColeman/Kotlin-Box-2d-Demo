package u0012604.box2ddemo.actors

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.tiled.tileHeight
import ktx.tiled.tileWidth
import u0012604.box2ddemo.box2d.Box2dConstants
import u0012604.box2ddemo.box2d.buildBasicPhysicsBodyFromSprite
import u0012604.box2ddemo.findNamedLocationCoordinates

class Player(map: TiledMap, override val world: World) : Actor {

    private val tex = Texture("Images/Sprite.png".toInternalFile())

    override val sprite = Sprite(tex).apply {
        val pos = findNamedLocationCoordinates(map, "Objects", "PlayerStartPos")

        // 1. Scale the starting size to METERS
        val w = map.tileWidth.toFloat() / Box2dConstants.PIXELS_PER_METER
        val h = map.tileHeight.toFloat() / Box2dConstants.PIXELS_PER_METER
        setSize(w, h)

        // 2. Set the rotation point to the center of the sprite
        setOriginCenter()

        // 3. Scale the starting position to METERS
        // Note: Body creation usually handles the final position,
        // but this keeps the sprite synced from frame 0.
        setPosition(
            (pos.x / Box2dConstants.PIXELS_PER_METER) - w / 2f,
            (pos.y / Box2dConstants.PIXELS_PER_METER) - h / 2f
        )
    }

    override val body: Body = buildBasicPhysicsBodyFromSprite(world, this)

    private val halfSpriteWidth = sprite.width / 2f
    private val halfSpriteHeight = sprite.height / 2f

    // ---------------------------------------------------------
    // update()
    // Apply Box2d physics to the sprite's position.
    // ---------------------------------------------------------
    override fun update() {
        body.let {
            // Box2D position is center, Sprite position is bottom-left
            sprite.setPosition(
                it.position.x - halfSpriteWidth,
                it.position.y - halfSpriteHeight
            )

            // Convert Radians to Degrees for LibGDX
            sprite.rotation = it.angle * MathUtils.radDeg
        }
    }

    override fun dispose() {
        tex.disposeSafely()
    }
}
