#ifndef ACcodes_h
#define ACcodes_h

#include <Arduino.h>

// #define H_Fan		0x8E76897
// #define M_Fan		0x8E7708F
// #define L_Fan		0x8E750AF
// #define Plus		0x8E7A857
// #define Minus		0x8E7906F
// #define Cool		0x8E728D7
// #define Dry			0x8E730CF
// #define Fan			0x8E710EF
// #define Timer 	0x8E7807F
// #define On_Off	0x8E700FF

#define Mode_Auto		0x0000060A
#define Mode_Cool		0x8000060A
#define Mode_Dry		0x4000060A
#define Mode_Fan		0xC000060A
// #define Mode_Heat		0x2000060A

#define Switch_On		0x1000060A
#define Switch_Off	0x0000060A

#define Temp_16			0x0000060A
#define Temp_17			0x0080060A
#define Temp_18			0x0040060A
#define Temp_19			0x00C0060A
#define Temp_20			0x0020060A
#define Temp_21			0x00A0060A
#define Temp_22			0x0060060A
#define Temp_23			0x00E0060A
#define Temp_24			0x0010060A
#define Temp_25			0x0090060A
#define Temp_26			0x0050060A
#define Temp_27			0x00D0060A
#define Temp_28			0x0030060A
#define Temp_29			0x00B0060A
#define Temp_30			0x0070060A

#define Fan_Auto		0x0000060A
#define Fan_1				0x0800060A
#define Fan_2				0x0400060A
#define Fan_3				0x0C00060A
#define Fan_4				0x0C000E0A

#define Swing_on		0x0200060A
#define Swing_off		0x0000060A

// #define Sleep_on		0x0100060A
// #define Sleep_off		0x0000060A

#endif
