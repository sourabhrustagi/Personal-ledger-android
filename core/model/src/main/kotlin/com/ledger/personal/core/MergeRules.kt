package com.ledger.personal.core

/** Result of comparing one field from two copies of a record. */
sealed interface FieldMerge {
    /** Both copies hold the same value. */
    data class Kept(val value: String) : FieldMerge
    /** The two copies disagree, so a person has to choose. */
    data class Conflict(val left: String, val right: String) : FieldMerge
}

/** Keeps the value when both sides match, otherwise reports a conflict. */
fun mergeField(left: String, right: String): FieldMerge =
    if (left == right) FieldMerge.Kept(left) else FieldMerge.Conflict(left, right)
