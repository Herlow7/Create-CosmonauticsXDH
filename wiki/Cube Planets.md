Cube planets are the most straightforward celestial body. They are loaded from `/data/<namespace>/universe_planets/<name>.json`.

Cube planets are constructed in a "priority override" system, where the non-empty fields of higher priority files override the fields of lower priority files. As such, all fields are by default optional and only present fields will override the settings of lower priority files. However, the planet will fail to construct and an error will be logged if critical information is missing after the composition is finished. Note that celestial positions and rotations cannot be partially overridden, but must be swapped out completely.

Certain fields are "paired" with other fields. This means that at least one of the paired fields must be present, but some will override others if present. As an example, either `mu` or `acceleration_at_surface` must be provided, but `mu` will take priority over `acceleration_at_surface`.

While Cube Planets can be linked to custom dimensions, we do not provide any tools to add dimensions. However, datapacks are entirely capable of adding custom dimensions, and tutorials exist for this.

## Fields

**`parent`** (critical): The name of the parent body. If the planet has no parent body, such as `sol` by default, use `root` here.

**`name`** (required): The name field is extremely important. In the vast majority of cases it will be the same as the file name. The name field is used to determine what files stack on each other during the "priority override" phase.

**`radius`** (critical): The radius of the cube planet, equivalent to half its side length.

**`mu`** (critical, paired with `acceleration_at_surface`): the standard gravitational parameter (μ) of this cube planet.

**`acceleration_at_surface`** (critical, paired with `mu`): the acceleration that physics objects are subject to at a distance of `radius` from the center of the cube planet.

**`position`** (critical): A [Celestial Position](https://github.com/CosmonauticsTeam/Create-Cosmonautics/wiki/Celestial-Positions). Cannot be partially overridden.

**`rotation`** (critical): A [Celestial Rotation](https://github.com/CosmonauticsTeam/Create-Cosmonautics/wiki/Celestial-Rotations). Cannot be partially overridden.

**`dimension_data`** (optional): A compound object consisting of [these fields](https://github.com/CosmonauticsTeam/Create-Cosmonautics/wiki/Cube-Planets#Dimension-Data-Fields). Each field can be overridden separately.

**`planet_extras`** (optional): A compound object consisting of [these fields](https://github.com/CosmonauticsTeam/Create-Cosmonautics/wiki/Cube-Planets#Planet-Extras-Fields). Each field can be overridden separately.

**`planet_texture`** (critical): A [Texture Definition](https://github.com/CosmonauticsTeam/Create-Cosmonautics/wiki/Texture-Definition). Cannot be partially overridden.

**`priority`** (optional, default `1000`): The priority of this file in the "priority override" phase. Must be an integer. Cube planets added by the base mod have a priority of 0.

**`disabled`** (optional, default `false`): Whether this planet should be loaded. Useful for disabling planets added by the base mod.

### Dimension Data Fields

**`linked_dimension`** (critical): The resource location of the dimension linked to this cube planet.

**`allowed_transfer`** (optional, default `none`): Which transitions between deep space and the dimension are allowed. Options are `all`, `none`, `to_space`, and `to_dimension`.

**`dimension_transfer_height`** (optional, default `20_000`): The height of the transition point. Applies to both deep space and the dimension. Must be an integer.

**`atmosphere_composition`** (optional): A map of heights to atmosphere composition flags. Each height in the map controls the "bucket" of altitudes between it and the height below. Associated flags are a collection of supported flags. Options are `low_density`, `drowning`. Currently `low_density` controls whether the quadruple efficiency bonus of copper leg thrusters applies, while `drowning` activates our standard no atmosphere behavior of causing the player to start drowning.

**`render_universe_in_dimension`** (optional, default `false`): Whether the universe should render in the dimension's skybox. The dimension's default skybox must be manually disabled elsewhere.

**`dimension_day_time_controller_name`** (optional): The name of the celestial body that will be compared against to determine the time of day in the linked dimension. Recommended if the universe is rendered in the dimension.

**`apply_gravity_correction_to_entities_in_dimension`** (optional, default `false`): Whether a gravity correction should be applied to entities in the dimension. Normal gravity is assumed to be `11m/s^2` at surface because this is the Sable default, and the correction will be proportional to the ratio between this and the gravity found in the dimension's physics data supplied by Sable.

**`entity_drag_multiplier`** (optional): A list of bezier control points for controlling entity drag with altitude, equivalent to Sable's native `pressure_function` in dimension physics data. Each point has `altitude` (y-level), `value` (multiplier to entity drag at that altitude), and `slope` (rate of change). Omit this field for no modifications to entity drag.

### Planet Extras Fields

**`is_star`** (optional, default `false`): Whether the cube planet is a star and should have a star render.

**`has_clouds`** (optional, default `false`): Whether the cube planet should have clouds render above its surface.

**`light_source_name`** (optional): The name of the celestial body that will be used to determine shadows cast across the planet's surface.
