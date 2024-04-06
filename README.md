## WETS-OG

A fork of [WETS](https://github.com/emilyy-dev/wets) **without Fabric support** maintained for [TrueOG](https://true-og.net/).

The default blocks per tick is 100.

Spreads WorldEdit operations across multiple ticks.

Semi-configurable, single command `/wets` (`/worldedit-tick-spreader`), one permission (`wets`), two possible arguments:
* `/wets (sorted | not-sorted)` - whether blocks will be placed in a sorted arrangement or not. Sorted is default.
* `/wets <blocks-per-tick>` - Changes the amount of blocks that will be placed per tick.
Negative numbers result in a lot of blocks per tick (9223372036854775807). Default is 1.

Sorted placing and block count per tick, affected by commands, are per-player.

## License
Project licensed under the MIT license (also known as Expat license).

The original project which supports Fabric API can be found [here](https://github.com/emilyy-dev/wets).
