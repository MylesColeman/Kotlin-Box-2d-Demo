package u0012604.box2ddemo.box2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.ChainShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import u0012604.box2ddemo.actors.Actor


fun buildBox2dWorld(
    map: TiledMap,
    gravity: Vector2,
    collisionBodiesLayerName: String,
    simulationStep: Float,
    velocityIterations: Int,
    positionIterations: Int,
    pixelsPerMeter: Float
) : World {

    val world = World(gravity, true)

    world.step(simulationStep, velocityIterations, positionIterations)

    val bodies = mutableListOf<Body>()

    // If there is a layer in the map specified by 'collisionBodiesLayerName', then
    // use this to build the static collision polygons
    map.layers.get(collisionBodiesLayerName)?.let {
        it.objects.forEach { polygonDef ->
            // Rectable or non-regular polygon?
            when(polygonDef) {
                is RectangleMapObject -> buildRectangularBody(world, pixelsPerMeter, polygonDef)
                is PolygonMapObject -> buildPolygonBody(world, pixelsPerMeter, polygonDef)
                else -> null
            }?.let { body -> // Only add non-null BodyDef!
                bodies.add(
                    body.apply {
                        type = BodyDef.BodyType.StaticBody
                        userData = Box2dUserDataTypes.MAP_COLLISION_BOX
                    })
            }
        }
    }

    return world
}

internal fun buildRectangularBody(
    world: World,
    pixelsPerMeter: Float,
    rectMapObj: RectangleMapObject) : Body =
    rectMapObj.rectangle.let { rect ->
        world.createBody(
            BodyDef().apply {
                position.x = (rect.x + rect.width / 2) / pixelsPerMeter
                position.y = (rect.y + rect.height / 2) / pixelsPerMeter
            }).also {
            val shape = PolygonShape().apply {
                setAsBox(rect.width / 2 / pixelsPerMeter, rect.height / 2 / pixelsPerMeter)
            }
            it.createFixture(shape, 0f).apply {
                userData = it
            }

            shape.dispose()
        }
    } // Returns libGdx Body type

internal fun buildPolygonBody(
    world: World,
    pixelsPerMeter: Float,
    collisionPolygon: PolygonMapObject) : Body =
    collisionPolygon
        .polygon
        .transformedVertices
        .map { it / pixelsPerMeter }
        .toFloatArray().let {
                vertices ->
            Gdx.app.log("CollisionMap::buildPolygonBody", "VERTICES: ${vertices}")
            world.createBody(
                BodyDef().apply { position.set(0f, 0f) }
            ).also {
                val shape = ChainShape().apply { createChain(vertices) }
                it.createFixture(
                    shape,
                    0f).apply {
                    userData = it
                }

                shape.dispose()
            }
        }// Returns libGdx Body type

fun buildBasicPhysicsBodyFromSprite(
    world: World,
    actor: Actor,
    isStatic: Boolean = false) : Body {
    // Physics setup
    val hw = actor.sprite.width / 2
    val hh = actor.sprite.height / 2
    val x = actor.sprite.x
    val y = actor.sprite.y

    return world.createBody(
        BodyDef().apply {
            type =  if(isStatic)    BodyDef.BodyType.StaticBody
            else            BodyDef.BodyType.DynamicBody
            position.set(x + 2 * hw, y + 2 * hh)
        })
        .apply {
            val polyShape = PolygonShape().apply {
                setAsBox(hw, hh)
            }

            createFixture(
                FixtureDef().apply {
                    shape = polyShape
                    density = 1f
                }).apply {
                    userData = actor
            }

            polyShape.dispose()
        }
}
