#import "RNGAMUtils.h"

NSArray *__nullable RNGAMProcessTestDevices(NSArray *__nullable testDevices, id _Nonnull simulatorId)
{
    if (testDevices == NULL) {
        return testDevices;
    }
    NSInteger index = [testDevices indexOfObject:@"SIMULATOR"];
    if (index == NSNotFound) {
        return testDevices;
    }
    NSMutableArray *values = [testDevices mutableCopy];
    [values removeObjectAtIndex:index];
    [values addObject:simulatorId];
    return values;
}
