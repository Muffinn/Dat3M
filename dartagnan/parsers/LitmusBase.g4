grammar LitmusBase;

import BaseLexer;

@header{
import dartagnan.asserts.*;
import dartagnan.expression.op.COpBin;
import dartagnan.expression.IntExprInterface;
import dartagnan.program.*;
import dartagnan.program.memory.Location;
import dartagnan.expression.IConst;
import dartagnan.parsers.utils.ProgramBuilder;
}

@parser::members{
    ProgramBuilder pb;
}

// These rules must be overwritten in child grammars
variableDeclaratorList                      :;
program                                     :;
variableList                                :;
assertionValue returns [IntExprInterface v] :;


main
    :    LitmusLanguage ~(LBrace)* variableDeclaratorList program variableList? assertionFilter? assertionList? comment? EOF
    ;

assertionFilter
    :   AssertionFilter a = assertion Semi?
    ;

assertionList
    :   AssertionExists a = assertion Semi?
    |   AssertionExistsNot a = assertion Semi?
    |   AssertionForall a = assertion Semi?
    |   AssertionFinal a = assertion Semi? assertionListExpectationList
    ;

assertion
    :   LPar assertion RPar
    |   AssertionNot assertion
    |   assertion AssertionAnd assertion
    |   assertion AssertionOr assertion
    |   assertionValue assertionCompare assertionValue
    ;

assertionListExpectationList
    :   AssertionWith (assertionListExpectation)+
    ;

assertionListExpectation
    :   AssertionListExpectationTest Colon (AssertionExists | AssertionExistsNot) Semi
    ;

assertionCompare
    :   (Equals | EqualsEquals)
    |   NotEquals
    |   GreaterEquals
    |   LessEquals
    |   Less
    |   Greater
    ;

threadId returns [String id]
    :   t = ThreadIdentifier {$id = $t.text.replace("P", "");}
    |   t = DigitSequence {$id = $t.text;}
    ;

comment
    :   LPar Ast .*? Ast RPar
    ;

AssertionListExpectationTest
    :   'tso'
    |   'cc'
    |   'optic'
    |   'default'
    ;

AssertionAnd
    :   '/\\'
    |   '&&'
    ;

AssertionOr
    :   '\\/'
    |   '||'
    ;

AssertionExistsNot
    :   '~exists'
    |   '~ exists'
    ;

AssertionExists
    :   'exists'
    ;

AssertionFinal
    :   'final'
    ;

AssertionForall
    :   'forall'
    ;

AssertionFilter
    :   'filter'
    ;

AssertionNot
    :   Tilde
    |   'not'
    ;

AssertionWith
    :   'with'
    ;

Locations
    :   'locations'
    ;

EqualsEquals
    :   '=='
    ;

NotEquals
    :   '!='
    ;

LessEquals
    :   '<='
    ;

GreaterEquals
    :   '>='
    ;

ThreadIdentifier
    :   'P' DigitSequence
    ;

// Must be overwritten in child grammars
LitmusLanguage  :   'BaseLitmusLanguage';

Identifier
    :   (Letter)+ (Letter | Digit)*
    ;

DigitSequence
    :   Minus? Digit+
    ;

fragment
Digit
    :   [0-9]
    ;

fragment
Letter
    :   [A-Za-z]
    ;

Whitespace
    :   [ \t]+
        -> skip
    ;

Newline
    :   (   '\r' '\n'?
        |   '\n'
        )
        -> skip
    ;

BlockComment
    :   '(*' .*? '*)'
        -> skip
    ;

ExecConfig
    :   '<<' .*? '>>'
        -> skip
    ;