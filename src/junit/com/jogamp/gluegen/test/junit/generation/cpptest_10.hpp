typedef enum SomeEnum {
  ConstEnumValue00 = 16,
  ConstEnumValue01 = (1 << ConstEnumValue00) - 1,
  ConstEnumValue02 = 10-1,
  ConstEnumValue03 = 10 - 1,
  ConstEnumValue04 = 10 - 11,
  ConstEnumValue05 = -2,
  ConstEnumValue06 = - 2,
  ConstEnumValueXX = 0
} SomeEnum;
