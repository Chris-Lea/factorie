/* Copyright (C) 2008-2014 University of Massachusetts Amherst.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://github.com/factorie
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */
package cc.factorie.app.nlp.coref
import cc.factorie.app.nlp._
import cc.factorie.app.nlp.phrase._
import cc.factorie.app.nlp.ner._

/** A dead-simple deterministic coreference system that operates only on named entities
    and resolves coreference only by exact string match. */
object DeterministicNamedCoref extends DocumentAnnotator {
  def prereqAttrs: Seq[Class[_]] = ConllProperNounPhraseFinder.prereqAttrs
  def postAttrs = Seq(classOf[WithinDocCoref])
  def tokenAnnotationString(token:Token): String = ???
  def process(document: Document) = {
    val phrases = ConllProperNounPhraseFinder(document)
    val coref = new WithinDocCoref(document)
    for (phrase <- phrases) {
      val targetString = phrase.tokensString(" ")
      // Find an entity whose canonical mention is an exact string match
      val entityOption = coref.entities.find(_.canonicalMention.string == targetString)
      if (entityOption.isDefined) coref.addMention(phrase, entityOption.get)
      else { val entity = coref.newEntity(); val mention = coref.addMention(phrase, entity); entity.canonicalMention = mention }
    }
    document.attr += coref
    document
  }

}
