/* Copyright (C) 2008-2010 Univ of Massachusetts Amherst, Computer Science Dept
 This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
 http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
 This software is provided under the terms of the Eclipse Public License 1.0
 as published by http://www.opensource.org.  For further information,
 see the file `LICENSE.txt' included with this distribution. */

package cc.factorie.example

import scala.collection.mutable.{ArrayBuffer}
import scala.util.matching.Regex
import scala.io.Source
import java.io.File
import cc.factorie._

/** A raw document classifier without using any of the facilities of cc.factorie.application.DocumentClassification,
    and without using the entity-relationship language of cc.factorie.er.  
    Furthermore, use conditional maximum likelihood training (and parameter updating with the optimize package)
    By contrast, see example/DocumentClassifier2. */
object DocumentClassifier3 {
  
  class Document(file:File) extends BinaryVectorVariable[String] {
    var label = new Label(file.getParentFile.getName, this)
    // Read file, tokenize with word regular expression, and add all matches to this BinaryVectorVariable
    "\\w+".r.findAllIn(Source.fromFile(file).mkString).foreach(regexMatch => this += regexMatch.toString)
  }
  class Label(name:String, val document:Document) extends LabelVariable(name) 

  val model = new Model(
    /** Bias term just on labels */
    new TemplateWithDotStatistics1[Label], 
    /** Factor between label and observed document */
    new TemplateWithDotStatistics2[Label,Document] {
      def unroll1 (label:Label) = Factor(label, label.document)
      def unroll2 (token:Document) = throw new Error("Document values shouldn't change")
    }
  )

  val objective = new Model(new LabelTemplate[Label])

  def main(args: Array[String]) : Unit = {
    if (args.length < 2) 
      throw new Error("Usage: directory_class1 directory_class2 ...\nYou must specify at least two directories containing text files for classification.")

    // Read data and create Variables
    var documents = new ArrayBuffer[Document];
    for (directory <- args) {
      val directoryFile = new File(directory)
      if (! directoryFile.exists) throw new IllegalArgumentException("Directory "+directory+" does not exist.")
      for (file <- new File(directory).listFiles; if (file.isFile)) {
        //println ("Directory "+directory+" File "+file+" documents.size "+documents.size)
        documents += new Document(file)
      }
    }
    
    // Make a test/train split
    val (testSet, trainSet) = documents.shuffle.split(0.5)
    var trainVariables = trainSet.map(_ label)
    var testVariables = testSet.map(_ label)
    (trainVariables ++ testVariables).foreach(_.setRandomly())

    // Train and test
    val trainer = new LogLinearMaximumLikelihood(model)
    trainer.process(trainVariables.map(List(_)))
    val predictor = new VariableSettingsMaximizer[Label](model)
    predictor.processAll(trainVariables)
    predictor.processAll(testVariables)
    println ("Train accuracy = "+ cc.factorie.defaultObjective.aveScore(trainVariables))
    println ("Test  accuracy = "+ cc.factorie.defaultObjective.aveScore(testVariables))

  }
}



