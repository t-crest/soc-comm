# SPI

Play with SPI devices.

See the `Makefile` for the hardware and test targets.

For running SPI simulation the two Verilog files need to be added in src/test/verilog.
Due to copyright reasons they are not included in this repository.

Pmod pinout can be found at:

## Links

* http://ww1.microchip.com/downloads/en/devicedoc/s71271_04.pdf
* http://www.cypress.com/file/316661/download
* https://ww1.microchip.com/downloads/aemDocuments/documents/MPD/ProductDocuments/DataSheets/23A102423LC1024-1-Mbit-SPI-Serial-SRAM-with-SDI-SQI-Interface-20005142.pdf
* SRAM example: https://www.mouser.dk/datasheet/2/1127/APM_PSRAM_QSPI_APS1604M_SQR_v2_5_PKG-1954861.pdf
* Interesting IO boards: https://www.mikroe.com/flash-2-click
* Slave implementation: https://github.com/nandland/spi-slave/blob/master/Verilog/source/SPI_Slave.v

### Simulation Models

* https://github.com/YosysHQ/picorv32/blob/main/picosoc/spiflash.v
* https://www.cypress.com/verilog/s25fl256s-verilog (results in a .exe to expand)
    * see also: https://github.com/pulp-platform/pulp/tree/master/rtl/vip/spi_flash
* https://www.infineon.com/cms/en/product/memories/nor-flash/semper-nor-flash-family/semper-nor-flash/?gad_source=1&gclid=Cj0KCQjwlZixBhCoARIsAIC745BlF0r2uRRUs_tH-uFewg_eQ0zYvxKVwG_Ea77YYKOk0AGSk2u4oVQaAmdSEALw_wcB&gclsrc=aw.ds#!designsupport

### Other Stuff

* https://www.mouser.dk/ProductDetail/Infineon-Technologies/EVAL-S25HL512T?qs=T%252BzbugeAwjiZrIcBHC%252BHKw%3D%3D&utm_source=octopart&utm_medium=aggregator&utm_campaign=727-EVAL-S25HL512T&utm_content=Infineon&_gl=1*1tqxjz0*_ga*MTA4ODI1ODYwNC4xNzEzNzg5NDgw*_ga_15W4STQT4T*MTcxMzgwNDI4MC4zLjAuMTcxMzgwNDI4MC42MC4wLjA.
* SRAM on a PCB: https://www.mouser.dk/ProductDetail/Mikroe/MIKROE-1902?qs=mzRxyRlhVdsdV%2FIU5dBGoQ%3D%3D
* board: https://www.mouser.dk/datasheet/2/272/sram-click-manual-v100-1487260.pdf
* See also (new ?) Click shield versions: https://www.mikroe.com/click/storage/sram

## TODO

* Connect Arduino and/or ESP32
    * https://www.mouser.lu/new/adafruit/adafruit-huzzah32-esp32-dev-boards/
* Are there better SPI simulation models? Those working with Verilator?
* Could this be a good example for cosimulation (UVM like?)
* Can I boot from a microSD card, just in hardware?
* Do bit banging via serial port to SPI on the FPGA (PMOD)
* I can explore more SPI on the Nexys:
    * SD micro connector
    * Accelerometer
