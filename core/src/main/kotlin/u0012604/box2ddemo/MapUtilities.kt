package u0012604.box2ddemo

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2

// ----------------------------------------------------------
// findNamedLocationCoordinates()
// ----------------------------------------------------------
fun findNamedLocationCoordinates(
    map: TiledMap,
    mapLayer: String,
    namedLocation: String) : Vector2 =
    with(map.layers.get(mapLayer).objects.get(namedLocation)) {
        Vector2(
            (properties.get("x") as Float),
            (properties.get("y") as Float)
        )
    }
