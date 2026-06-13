Texture definitions are the primary way that textures are defined in the universe.

### Texture File
A pointer to a standard image file.

**`type`** (required): `resloc`

**`texture`** (required): The resource location of the image. Note that this must be **fully qualified**, meaning that the `texture` folder needs to be included in the path as well.

### Biome Sampling
An image generated dynamically based on biome data.

**`type`** (required): `biome_sampler`

**`fallback_red`** (required): The red hex component of the fallback color. Ranges from 0 to 255.

**`fallback_green`** (required): The green hex component of the fallback color. Ranges from 0 to 255.

**`fallback_blue`** (required): The blue hex component of the fallback color. Ranges from 0 to 255.

**`colors`** (required): A list of compound objects, each consisting of the following fields:

>**`matching_biomes`** (optional): A list of biomes that match to this color.
>
>**`matching_tags`** (optional): A list of biome tags that match to this color.
>
>**`red`** (required): The red hex component of the color. Ranges from 0 to 255.
>
>**`green`** (required): The green hex component of the color. Ranges from 0 to 255.
>
>**`blue`** (required): The blue hex component of the color. Ranges from 0 to 255.
>
>**`priority`** (optional, default `0`): The priority of this color during matching. Only the highest priority matched color will have an effect.
>
>**`flags`** (optional): A list of flags for this color. Options are `ocean`. This mostly affects how the 3d-esque post-processing for the render interprets the color.